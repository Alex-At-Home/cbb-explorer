package org.piggottfamily.cbb_explorer

import ammonite.ops._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
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

   def main(args: Array[String]): Unit = {

      // Command line args processing

      if (args.length < 2) {
         println("""
         |--in=<<html-file-to-read>>
         |--out=<<out-file-in-which-JSON-output-is-placed>
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

      // Build team LUT

      val team_lut_str = Source.fromURL(getClass.getResource("/transfer_team_lookup.csv")).mkString         
      type TeamLutEntry = (String, String)
      val team_lut: Map[String, String] = team_lut_str.asCsvReader[TeamLutEntry](rfc).toList.flatMap(_.toOption).toMap

      // Get Recruits

      val recruits_file = Path(in_file)
      val recruits_html = read.lines(recruits_file).mkString("\n")

      val recruits = RecruitParser.get_early_declarations(in_file, recruits_html).getOrElse(List()).map { recruit =>
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
         val profile = recruit.rating match {
            case r if r >= 0.995 => "5*/Lotto"
            case r if r >= 0.990 => "5*"
            case r if r >= 0.980 => "4*/T40ish"
            case r if r >= 0.960 => "4*"
            case r if r >= 0.910 => "3.5*/T150ish"
            case _ => "3*"
         }
         val preproc_team = recruit.team.replace("State", "St.").replace("’", "'")
         val translated_team = team_lut.getOrElse(preproc_team, preproc_team)

         val name_frags = recruit.name.split(" ", 2) // switch from "Player One" to "One, Player"

         recruit.copy(
            name = if (name_frags.length > 1) s"${name_frags.drop(1).mkString(" ")}, ${name_frags(0)}" else recruit.name,
            team = translated_team,
            pos = positional_role,
            profile = Some(profile)
         )
      }

      System.out.println(s"BuildRecruitLookup: Ingested [${recruits.size}] recruits")


      // Get a list of teams and read in their rosters:

      val storage_controller = new StorageController()

      // Parse the names and find the codes

      case class MinimalRecruitInfo(pos: String, pr: String, c: String)

      val transfer_codes_to_team: Map[String, Map[String, MinimalRecruitInfo]] = recruits
         .groupBy(_.team).mapValues { rr => rr.map(r => {
            val code = ExtractorUtils.build_player_code(r.name, team = None).code;
            (r.name, MinimalRecruitInfo(r.pos, r.profile.getOrElse("UR"), code))
         }).toMap }

      val printer = Printer.noSpaces.copy(dropNullValues = true)
      write.over(Path(out_path), printer.pretty(transfer_codes_to_team.asJson))
   }
}