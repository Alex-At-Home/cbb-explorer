package org.piggottfamily.cbb_explorer

import java.nio.file.{Path, Paths}
import org.piggottfamily.cbb_explorer.utils.FileUtils
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.models.offseason._
import org.piggottfamily.cbb_explorer.controllers.kenpom.ParserController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import scala.util.Try
import java.net.URLEncoder
import org.joda.time.DateTime
import scala.util.matching.Regex
import org.piggottfamily.cbb_explorer.utils.parsers.offseason.RecruitParser
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.TeamIdParser
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.LineupErrorAnalysisUtils
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.ExtractorUtils
import scala.io.Source
import scala.util.{Try, Success, Failure}

import kantan.csv._
import kantan.csv.ops._

object BuildRecruitLookup {

   // Mid majors with T200 recruits
   val mid_majors = Set(
      "App State", "UC Santa Barbara", "Ohio", "New Mexico", "Harvard", 
      "Pepperdine", "Nevada", 
   )
   // Mid majors with T200 recruits
   val mid_high_majors = Set(
      "Davidson", "San Diego St.",
      "Fordham", "Cincinatti", "Saint Mary's (CA)", 
   )

   def main(args: Array[String]): Unit = {

      // Command line args processing
      // (inspect element, copy as html from top level, use pbpaste to write to file)
      // java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" org.piggottfamily.cbb_explorer.BuildRecruitLookup --in=$PBP_OUT_DIR/in_file.html --out=$PBP_OUT_DIR/out_file.json --year=2024

      if (args.length < 2) {
         println("""
         |--in=<<html-file-to-read>>
         |--out=<<out-file-in-which-JSON-output-is-placed>
         |[--rosters=<<json-roster-dir>>]
         |--year=<<year-of-the-rosters>>
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
      val maybe_roster_dir = args
         .map(_.trim).filter(_.startsWith("--rosters="))
         .headOption.map(_.split("=", 2)(1))

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

      // Get a list of teams and read in their rosters:

      val recruits_file = Paths.get(in_file)
      val recruits_html = FileUtils.read_lines_from_file(recruits_file).mkString("\n")

      val storage_controller = new StorageController()

      val roster_vs_team = RecruitParser.get_recruits(in_file, recruits_html).getOrElse(List()).map(_.team).distinct.flatMap { team =>
         val preproc_team = team.replace("State", "St.").replace("’", "'")
         val translated_team = team_lut.getOrElse(preproc_team, preproc_team)

         maybe_roster_dir.flatMap { roster_dir =>
            Try {
               storage_controller.read_roster(Paths.get(roster_dir).resolve(s"Men_$year").resolve(s"$translated_team.json"))
            }.recoverWith {
               case error =>
                  System.out.println(s"Failed to ingest [$team][$translated_team]: $error")
                  Failure(error)
            }.map(roster => (translated_team, LineupEvent( //Lots of dummy fields, only "players" field is populated
               DateTime.now(), Game.LocationType.Home, 0.0, 0.0, 0.0, LineupEvent.ScoreInfo.empty, 
               TeamSeasonId(TeamId(translated_team), Year(year)), TeamSeasonId(TeamId(translated_team), Year(year)), LineupEvent.LineupId("none"),
               roster.values.toList.map(_.player_code_id), 
               Nil, Nil, Nil, LineupEventStats.empty, LineupEventStats.empty, None
            ))).toOption
         }
      }.toMap

      System.out.println(s"BuildRecruitLookup: Ingested [${roster_vs_team.size}] teams, [${roster_vs_team.values.flatMap(_.players).size}] players")

      // Get Recruits

      val recruits = RecruitParser.get_recruits(in_file, recruits_html).getOrElse(List()).flatMap { recruit =>
         val positional_role = recruit.pos match {
            case "C" => "C"
            case "PF" if recruit.height_in >= 81 => "PF/C" //6-9+
            case "PF" if recruit.height_in <= 78 => "WF" //6-6-
            case "PF" => "S-PF"
            case "SF" if recruit.height_in > 78 => "WF" //6-7+
            case "SF" => "WG"
            case "SG" => "WG"
            case "CG" => "CG"
            case "PG" => "PG"
            case other => "WF" //(a good generic guess otherwise!)
         }
         val (profile, adj_rating) = recruit.rating match {
            case r if r >= 0.995 => ("5*/Lotto", (r - 1.0)/0.005)
            case r if r >= 0.990 => ("5*", (r - 0.995)/0.005)
            case r if r >= 0.980 => ("4*/T40ish", (r - 0.985)/0.005)
            case r if r >= 0.960 => ("4*", (r - 0.970)/0.010)
            case r if r >= 0.910 => ("3.5*/T150ish", (r - 0.935)/0.025)
            case r => ("3*", Math.max(-1.0, (r - 0.885)/0.025))
         }
         val preproc_team = recruit.team.replace("State", "St.").replace("’", "'")
         val translated_team = team_lut.getOrElse(preproc_team, preproc_team)

         val name_frags = recruit.name.split(" ", 2) // switch from "Player One" to "One, Player"
         val translated_name = if (name_frags.length > 1) s"${name_frags.drop(1).mkString(" ")}, ${name_frags(0)}" else recruit.name
         
         val maybe_roster = roster_vs_team.get(translated_team)

         val tidied_name = maybe_roster.map { roster =>
            val tidy_ctx = LineupErrorAnalysisUtils.build_tidy_player_context(roster)
            val (fixed_player, _) = LineupErrorAnalysisUtils.tidy_player(translated_name, tidy_ctx)
            fixed_player
         }.getOrElse(translated_name)

         Some(recruit.copy(
            name = tidied_name,
            team = translated_team,
            rating = adj_rating,
            pos = positional_role,
            profile = Some(profile)
         )).filter { _ => (mid_majors.contains(recruit.team), mid_high_majors.contains(recruit.team)) match {
            case (true, false) => true //mid any 3* will do
            case (false, true) => recruit.rating >= 0.885 //mid-high, only count if T150+ (or toward the top of 3*)
            case _ => recruit.rating >= 0.945 //high, only count if 4*+ (or toward the top of 3*/T150)
         }}
      }

      System.out.println(s"BuildRecruitLookup: Ingested [${recruits.size}] recruits, post filter")

      // Parse the names and find the codes

      case class MinimalRecruitInfo(pos: String, pr: String, c: String, h: String, r: Long)
      def build_height(h_in: Int) = s"${h_in/12}-${h_in%12}"

      val transfer_codes_to_team: Map[String, Map[String, MinimalRecruitInfo]] = recruits
         .groupBy(_.team).mapValues { rr => rr.map(r => {
            val code = ExtractorUtils.build_player_code(r.name, team = None).code
            val adj_rtg_as_decimal = (r.rating*100).round
            (r.name, MinimalRecruitInfo(r.pos, r.profile.getOrElse("UR"), code, build_height(r.height_in), adj_rtg_as_decimal))
         }).toMap }

      val printer = Printer.noSpaces.copy(dropNullValues = true)
      FileUtils.write_file(Paths.get(out_path), printer.print(transfer_codes_to_team.asJson))
   }
}