package org.piggottfamily.cbb_explorer

import ammonite.ops._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.kenpom.ParserController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import scala.util.Try
import java.net.URLEncoder
import org.joda.time.DateTime
import scala.util.matching.Regex
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.TeamIdParser
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.LineupErrorAnalysisUtils
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.ExtractorUtils
import scala.io.Source
import scala.util.{Try, Success, Failure}

import kantan.csv._
import kantan.csv.ops._

object BuildTransferLookup {

   def main(args: Array[String]): Unit = {

      // Command line args processing

      if (args.length < 4) {
         println("""
         |--in=<<csv-file-to-read>>
         |--rosters=<<json-roster-dir>>
         |--out=<<out-file-in-which-JSON-output-is-placed>
         |--year=<<year-in-which-the-season-starts>>
         """)
         System.exit(-1)
      }
      val in_file = args.toList
         .map(_.trim).filter(_.startsWith("--in="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--in is needed")
         }
      val out_path = args
         .map(_.trim).filter(_.startsWith("--out="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--out is needed")
         }
      val roster_dir = args
         .map(_.trim).filter(_.startsWith("--rosters="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--rosters is needed")
         }
      val year = args
         .map(_.trim).filter(_.startsWith("--year="))
         .headOption.map(_.split("=", 2)(1))
         .map(_.toInt)
         .getOrElse {
            throw new Exception("--year is needed")
         }

      // Build team LUT

      val team_lut_str = Source.fromURL(getClass.getResource("/transfer_team_lookup.csv")).mkString         
      type TeamLutEntry = (String, String)
      val team_lut: Map[String, String] = team_lut_str.asCsvReader[TeamLutEntry](rfc).toList.flatMap(_.toOption).toMap

      // Read in transfer list from file as CSV
      val file = Path(in_file)
      val transfer_csv = read.lines(file).mkString("\n")
      type TransferEntry = (String, String, String, String, String, String, String, String, String, String)
         //stars/pos/name/class/ht/wt/eligible/jan-eligible/school/dest
      case class TransferInfo(name: String, team: String, dest: Option[String])


      val transfers = transfer_csv.asCsvReader[TransferEntry](rfc).toList.flatMap(_.toOption).flatMap { entry => 
         val preproc_name = entry._3
         val postproc_name = preproc_name.substring(0, preproc_name.size/2)
         val preproc_team = entry._9.replace("State", "St.")
         val preproc_dest = entry._10.replace("State", "St.")
         //some more tidy up:
         val name_frags = postproc_name.split(" ")
         val tidied_postproc_name = if (name_frags.size == 2) {
            s"${name_frags(0)}, ${name_frags(1)}" //(matches standard roster format)
         } else postproc_name
         val postproc_team = ExtractorUtils.remove_diacritics(team_lut.getOrElse(preproc_team, preproc_team))
         val postproc_dest = ExtractorUtils.remove_diacritics(team_lut.getOrElse(preproc_dest, preproc_dest))

         //Diag:
         //System.out.println(s"Player: [${postproc_name}][${postproc_team}][${entry._10}]")

         if (postproc_team != "NOT_D1") { // already found a destination or not a D1 player
            Some(TransferInfo(
               name = tidied_postproc_name,
               team = postproc_team,
               dest = Some(postproc_dest).filter(_ != "")
            ))
         } else {
            None
         }

      }.toList
      
      System.out.println(s"BuildTransferLookup: Ingested [${transfers.size}] transfers")

      // Get a list of teams and read in their rosters:

      val storage_controller = new StorageController()

      val roster_vs_team: Map[String, LineupEvent] = transfers.map(_.team).distinct.flatMap { team_name =>
         val encoded_team_name = URLEncoder.encode(team_name, "UTF-8").replace(" ", "+")
         Try {
            storage_controller.read_roster(Path(roster_dir) / s"Men_$year" / s"$encoded_team_name.json")
         }.recoverWith {
            case error =>
               System.out.println(s"Failed to ingest [$team_name][$encoded_team_name]: $error")
               Failure(error)
         }.map(roster => (team_name, LineupEvent( //Lots of dummy fields, only "players" field is populated
            DateTime.now(), Game.LocationType.Home, 0.0, 0.0, 0.0, LineupEvent.ScoreInfo.empty, 
            TeamSeasonId(TeamId(team_name), Year(year)), TeamSeasonId(TeamId(team_name), Year(year)), LineupEvent.LineupId("none"),
            roster.values.toList.map(_.player_code_id), 
            Nil, Nil, Nil, LineupEventStats.empty, LineupEventStats.empty, None
         ))).toOption
      }.toMap

      System.out.println(s"BuildTransferLookup: Ingested [${roster_vs_team.size}] teams, [${roster_vs_team.values.flatMap(_.players).size}] players")

      // Parse the names and find the codes

      case class TransferToFrom(f: String, t: Option[String])

      val transfer_codes_to_team: Map[String, List[TransferToFrom]] = transfers.flatMap { transfer_entry =>
         val maybe_roster = roster_vs_team.get(transfer_entry.team)
         maybe_roster.flatMap { roster =>
            val tidy_ctx = LineupErrorAnalysisUtils.build_tidy_player_context(roster)
            val (fixed_player, _) = LineupErrorAnalysisUtils.tidy_player(transfer_entry.name, tidy_ctx)
            val maybe_fixed_player_code = roster.players.find(_.id.name == fixed_player).map(_.code)
            maybe_fixed_player_code match {
               case None => 
                  //Useful debug
                  //System.out.println(s"Failed to parse [${transfer_entry.name}][${transfer_entry.team}] vs [${roster.players}]")
               case _ =>
            }

            maybe_fixed_player_code.map { player_code =>
               transfer_entry.copy(name = player_code)
            }
         }
      }.groupBy(_.name).mapValues { tt => tt.map(t => TransferToFrom(f = t.team, t = t.dest)) }

      System.out.println(s"BuildTransferLookup: successfully identified [${transfer_codes_to_team.values.flatten.size}] players")

      val printer = Printer.noSpaces.copy(dropNullValues = true)
      write.over(Path(out_path), printer.pretty(transfer_codes_to_team.asJson))
   }
}