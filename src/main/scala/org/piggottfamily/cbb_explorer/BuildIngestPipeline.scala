package org.piggottfamily.cbb_explorer

import ammonite.ops._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.kenpom.ParserController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import scala.util.Try
import java.net.URLDecoder
import scala.util.matching.Regex
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.TeamIdParser
import scala.io.Source
import scala.util.{Try, Success, Failure}

/** See scripts/build_ingest_pipeline.sh for usage info */
object BuildIngestPipeline {

  val h2ncaa_conf_map = Map(
    "acc" -> "ACC",
    "americaeast" -> "America East",
    "american" -> "AAC",
    "atlanticsun" -> "ASUN",
    "atlanticten" -> "Atlantic 10",
    "bigeast" -> "Big East",
    "bigsky" -> "Big Sky",
    "bigsouth" -> "Big South",
    "bigten" -> "Big Ten",
    "bigtwelve" -> "Big 12",
    "bigwest" -> "Big West",
    "colonial" -> "CAA",
    "conferenceusa" -> "C-USA",
    "horizon" -> "Horizon",
    "ivy" -> "Ivy League",
    "maac" -> "MAAC",
    "mac" -> "MAC",
    "meac" -> "MEAC",
    "mountainwest" -> "Mountain West",
    "mvc" -> "MVC",
    "nec" -> "NEC",
    "ovc" -> "OVC",
    "pactwelve" -> "Pac-12",
    "patriot" -> "Patriot",
    "sec" -> "SEC",
    "socon" -> "SoCon",
    "southland" -> "Southland",
    "summit" -> "Summit League",
    "sunbelt" -> "Sun Belt",
    "swac" -> "SWAC",
    "wac" -> "WAC",
    "wcc" -> "WCC"
  )
  val ncaa2h_conf_map = h2ncaa_conf_map.map(_.swap)

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println("""
        |--in=<<file-to-read>>
        |--out=<<out-dir-in-which-dir-structure-is-placed>>
        |[--gender=women]
        |[--confs=<<comma-separated-list-of-confs>>] (or "all")
        |[--year=<<year>>] (default=2021_22)
        |[--replace-existing=yes|no] (default=no)
        """)
      System.exit(-1)
    }
    val in_file = args.toList
      .map(_.trim).filter(_.startsWith("--in="))
      .headOption.map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--in is needed")
      }
    val out_dir = args
      .map(_.trim).filter(_.startsWith("--out="))
      .headOption.map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--out is needed")
      }

    val gender_prefix = args
      .map(_.trim).filter(_.startsWith("--gender="))
      .headOption.map(_.split("=", 2)(1))
      .filter(_ == "women")
      .map(_ + "_").getOrElse("")

    val maybe_confs = args
      .map(_.trim).filter(_.startsWith("--confs="))
      .headOption.map(_.split("=", 2)(1))
      .filter(_ != "all")
      .map(_.split(",").map(_.trim))

    val year_str = args
      .map(_.trim).filter(_.startsWith("--year="))
      .headOption.map(_.split("=", 2)(1))
      .getOrElse("2021_22")

    val replace_existing = args
      .map(_.trim).filter(_.startsWith("--replace-existing="))
      .headOption.map(_.split("=", 2)(1))
      .exists(_ == "yes")

    println("Starting cbb-explorer (BuildIngestPipeline):")

    // Read in template
    val template = Source.fromURL(getClass.getResource("/lineup-cli.template.sh")).mkString

    // Read in file
    val file = Path(in_file)
    val html_str = read.lines(file).mkString("\n")

    // Parse file
    val triples = TeamIdParser.get_team_triples(file.toString, html_str)
    val conf_strings = TeamIdParser.build_lineup_cli_array(triples.right.get)

    maybe_confs.map(_.toList).getOrElse(conf_strings.keySet.map(_.name))
      .map(c => h2ncaa_conf_map.get(c).getOrElse(c)) //(allow either format)
      .foreach { conf =>
        val conf_str = s"${gender_prefix}${ncaa2h_conf_map(conf)}"

        val to_render = template
          .replace("__YEAR_HERE__", year_str.take(4))
          .replace("__CONF_HERE__", conf_str)
          .replace("__TEAMIDS_HERE__", conf_strings(ConferenceId(conf)))

        // Create some dirs:
        val parent_path = Path(out_dir) / conf_str / year_str
        val new_file = parent_path / "lineups-cli.sh"
        mkdir! parent_path
        if (replace_existing) {
          write.over(new_file, to_render)
          println(s"Created/overwritten [$new_file]")
        } else {
          Try(write(new_file, to_render)) match {
            case Success(_) => println(s"Created [$new_file]")
            case Failure(_) => println(s"Skipped existing [$new_file]")
          }
        }
      }

  }
}
