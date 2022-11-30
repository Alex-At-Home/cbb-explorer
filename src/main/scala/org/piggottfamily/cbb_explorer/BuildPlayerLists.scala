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
import org.piggottfamily.cbb_explorer.utils.parsers.offseason.NbaDeclarationParser
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.TeamIdParser
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.LineupErrorAnalysisUtils
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.ExtractorUtils
import scala.io.Source
import scala.util.{Try, Success, Failure}

import kantan.csv._
import kantan.csv.ops._

/** Build leaderboard lookup strings */
object BuildPlayerLists {

   def main(args: Array[String]): Unit = {

      // Command line args processing

      if (args.length < 2) {
         println("""
         |--in=<<csv-file-to-read>>
         |--rosters=<<json-roster-dir>>
         """)
         System.exit(-1)
      }
      val in_file = args.toList
         .map(_.trim).filter(_.startsWith("--in="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--in is needed")
         }
      val roster_dir = args
         .map(_.trim).filter(_.startsWith("--rosters="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--rosters is needed")
         }

      // Build team LUT

      val team_lut_str = Source.fromURL(getClass.getResource("/player_builder_team_lookup.csv")).mkString         
      type TeamLutEntry = (String, String)
      val team_lut: Map[String, String] = team_lut_str.asCsvReader[TeamLutEntry](rfc).toList.flatMap(_.toOption).toMap
      val d1_teams: Set[String] = team_lut.keys.toSet

      // Read in player list from file as CSV
      val file = Path(in_file)
      val transfer_csv = read.lines(file).mkString("\n")
      type PlayerEntry = (String, String, String, String, String, String, String, String, String, String)
         //name,pos,height,birthplace,birthday,HS year,NBA year,school,ignore,country,
      case class PlayerInfo(name: String, team: String, fr_year: String)

      val players = transfer_csv.asCsvReader[PlayerEntry](rfc).toList.flatMap(_.toOption).flatMap { entry => 
         val postproc_name = entry._1
         val year = entry._7 // (needs to be 2017+)
         val preproc_team = entry._9

         if (year >= "2017") {

            //some more tidy up:
            val name_frags = postproc_name.split(" ")
            val tidied_postproc_name = if (name_frags.size == 2) {
               s"${name_frags(0)}, ${name_frags(1)}" //(matches standard roster format)
            } else postproc_name
            val postproc_team = ExtractorUtils.remove_diacritics(team_lut.getOrElse(preproc_team, 
               if (d1_teams(preproc_team)) preproc_team else "NOT_D1"
            ).replace("State", "St.").replace("App St.", "App State")) //(exception!)

            //Diag:
            //System.out.println(s"Player: [${postproc_name}][${postproc_team}][${entry._10}]")

            if (postproc_team != "NOT_D1") { // already found a destination or not a D1 player
               Some(PlayerInfo(
                  name = tidied_postproc_name,
                  team = postproc_team,
                  fr_year = year
               ))
            } else {
               None
            }
         } else {
            None
         }

      }.toList
      
      System.out.println(s"BuildPlayerLists: Ingested [${players.size}] players")

      // Get a list of teams and read in their rosters:

      val storage_controller = new StorageController()

      val all_years = List("2017", "2018", "2019", "2020", "2021", "2022")

      val roster_vs_team: Map[String, List[LineupEvent]] = players.map { player_entry =>
         val team_name = player_entry.team
         val encoded_team_name = URLEncoder.encode(team_name, "UTF-8").replace(" ", "+")
         val result = (team_name, all_years.filter(_ >= player_entry.fr_year).flatMap { year =>
            Try {
               storage_controller.read_roster(Path(roster_dir) / s"Men_$year" / s"$encoded_team_name.json")
            }.recoverWith {
               case error =>
                  //System.out.println(s"BuildPlayerLists: Failed to ingest [$team_name][$encoded_team_name]: $error")
                  Failure(error)
            }.map(roster => LineupEvent( //Lots of dummy fields, only "players" field is populated
               DateTime.now(), Game.LocationType.Home, 0.0, 0.0, 0.0, LineupEvent.ScoreInfo.empty, 
               TeamSeasonId(TeamId(team_name), Year(year.toInt)), TeamSeasonId(TeamId(team_name), Year(year.toInt)), LineupEvent.LineupId("none"),
               roster.values.toList.map(_.player_code_id), 
               Nil, Nil, Nil, LineupEventStats.empty, LineupEventStats.empty, None
            )).toOption
         })  
         if (result._2.isEmpty) {
            System.out.println(s"BuildPlayerLists: Failed to ingest [$team_name][$encoded_team_name]")
         }
         result
      }.filter(_._2.nonEmpty).toMap

      System.out.println(
         s"BuildPlayerLists: Ingested [${roster_vs_team.size}] teams, [${roster_vs_team.values.flatten.flatMap(_.players).toList.distinct.size}] players"
      )

      // Parse the names and find the codes

      val player_codes: List[String] = players.flatMap { player_entry =>
         val rosters = roster_vs_team.get(player_entry.team).getOrElse(Nil)
         val any_matches = rosters.flatMap { roster =>
            val tidy_ctx = LineupErrorAnalysisUtils.build_tidy_player_context(roster)
            val (fixed_player, _) = LineupErrorAnalysisUtils.tidy_player(player_entry.name, tidy_ctx)
            
            val maybe_fixed_player_code = roster.players.find(_.id.name == fixed_player).map(_.code)
            maybe_fixed_player_code match {
               case None => 
                  //Useful debug
                  //System.out.println(s"Failed to parse [${player_entry.name}][${player_entry.team}] vs [${roster.players}]")
               case _ =>
            }
            maybe_fixed_player_code
         }
         any_matches match {
            case Nil =>
               val all_players = rosters.flatMap(_.players).distinct
               //System.out.println(s"Failed to parse [${player_entry.name}][${player_entry.team}] vs [${all_players}]")               
            case _ => 
         }
         any_matches.headOption
      }

      System.out.println(s"BuildPlayerLists: successfully identified [${player_codes.size}] players")

      System.out.println(player_codes.mkString(","))
   }
}