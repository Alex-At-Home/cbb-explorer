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

/** Reads the ingest pipeline and builds a team list for the web page */
object ParseIngestPipeline {

  val ncaa_conf_map = Map( //(need to come up with a mid vs low-mid strategy, fill in XXXs when done!)
    "acc" -> "high",
    "american" -> "high",
    "americaeast" -> "XXX",
    "atlanticsun" -> "XXX",
    "atlanticten" -> "midhigh",
    "bigeast" -> "high",
    "bigsky" -> "mid",
    "bigsouth" -> "XXX",
    "bigten" -> "high",
    "bigtwelve" -> "high",
    "bigwest" -> "XXX",
    "conferenceusa" -> "mid",
    "colonial" -> "mid",
    "horizon" -> "mid",
    "ivy" -> "XXX",
    "maac" -> "XXX",
    "mac" -> "mid",
    "meac" -> "XXX",
    "mountainwest" -> "midhigh",
    "mvc" -> "mid",
    "nec" -> "XXX",
    "ovc" -> "XXX",
    "pactwelve" -> "high",
    "patriot" -> "mid",
    "sec" -> "high",
    "socon" -> "mid",
    "southland" -> "XXX",
    "summit" -> "mid",
    "sunbelt" -> "mid",
    "swac" -> "XXX",
    "wac" -> "XXX",
    "wcc" -> "midhigh",

    "misc_conf" -> "midhigh" //(treat as midhigh even if not true)
  )

  val year_mapper = Map(
    "2018" -> "2018/9",
    "2019" -> "2019/20",
    "2020" -> "2020/21",
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

    val sorter = (t2: (String, Seq[TeamInfo])) => s"${t2._2.headOption.map(_.conf)} ${t2._1}"
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
