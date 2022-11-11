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
import java.net.URLDecoder

/** Reads the ingest pipeline and builds a team list for the web page 
 * java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" org.piggottfamily.cbb_explorer.ParseIngestPipeline --in=$HOME/websites/NCAA_by_conf
*/
object ParseIngestPipeline {

  val ncaa_conf_map = Map( 
    "acc" -> "high",
    "bigeast" -> "high",
    "bigten" -> "high",
    "bigtwelve" -> "high",
    "pactwelve" -> "high",
    "sec" -> "high",

    "american" -> "midhigh",
    "atlanticten" -> "midhigh",
    "mountainwest" -> "midhigh",
    "wcc" -> "midhigh",

    "bigwest" -> "mid",
    "conferenceusa" -> "mid",
    "colonial" -> "mid",
    "horizon" -> "mid",
    "ivy" -> "mid",
    "mac" -> "mid",
    "mvc" -> "mid",
    "summit" -> "mid",
    "sunbelt" -> "mid",

    "bigsky" -> "midlow",
    "maac" -> "midlow",
    "ovc" -> "midlow",
    "patriot" -> "midlow",
    "socon" -> "midlow",
    "wac" -> "midlow",

    "americaeast" -> "low",
    "atlanticsun" -> "low",
    "bigsouth" -> "low",
    "meac" -> "low",
    "nec" -> "low",
    "southland" -> "low",
    "swac" -> "low",

    "misc_conf" -> "midhigh" //(treat as midhigh even if not true)
  )

  val year_mapper = Map(
    "2018" -> "2018/9",
    "2019" -> "2019/20",
    "2020" -> "2020/21",
    "2021" -> "2021/22",
    "2022" -> "2022/23",
  )

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println("""
        |--in=<<dir-to-read>>
        """)
      System.exit(-1)
    }
    val in_dir = args.toList
      .map(_.trim).filter(_.startsWith("--in="))
      .headOption.map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--in is needed")
      }

    val paths = ls.rec! Path(in_dir)
    val women_path_regex = ".*/women_([^/]+)/([0-9]+)/(.*)_[0-9.]+$".r
    val men_path_regex = ".*/([^/]+)/([0-9]+)/(.*)_[0-9.]+$".r
    case class TeamInfo(gender: String, conf: String, year: String, team: String)

    val teams = paths.map(_.toString).collect {
      case women_path_regex(conf, year, team) => TeamInfo("Women", conf, year, URLDecoder.decode(team, "UTF-8"))
      case men_path_regex(conf, year, team) => TeamInfo("Men", conf, year, URLDecoder.decode(team, "UTF-8"))
    }

    // Sort by team not conf then team:
    //val sorter = (t2: (String, Seq[TeamInfo])) => s"${t2._2.headOption.map(_.conf)} ${t2._1}"
    val sorter = (t2: (String, Seq[TeamInfo])) => s"${t2._1}"
    teams.groupBy(_.team).toList.sortBy(sorter).foreach { case (key, vals) =>
      println(s"""  "$key": [""")
      vals.filter(team => year_mapper.get(team.year).nonEmpty).sortBy(t => t.gender + t.year).foreach {
        case TeamInfo(gender, conf, year, team) =>
          val conf_to_use = if (gender == "Men") conf else s"women_${conf}"
          println(s"""      { team: "$team", year: "${year_mapper(year)}", gender: "$gender", index_template: "$conf_to_use", category: "${ncaa_conf_map(conf)}" },""")
      }
      println("   ],")
    }
    println("\n")
  }
}
