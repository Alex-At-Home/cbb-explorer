package org.piggottfamily.cbb_explorer

import ammonite.ops._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.ncaa.LineupController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import scala.util.Try
import java.net.URLDecoder

object BuildLineups {

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println("""
        |--in=<<in-dir-up-to-conf-then-year>>
        |--out=<<out-dir-in-which-files-are-placed>>
        |[--team]=<<only include teams matching this string>>
        |[--full] (includes player in/out and raw events)
        |[--from=<<filter-files-before-this-unix-timestamp>>]
        """)
      System.exit(-1)
    }
    val in_dir = args.toList
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
    val strip_unused_data =
      !args.toList.map(_.trim).exists(_ == "--full")

    val maybe_filter = args
      .map(_.trim).filter(_.startsWith("--from="))
      .headOption
        .map(_.split("=", 2)(1))
        .map(_.toLong)

    val maybe_team_selector = args
      .map(_.trim).filter(_.startsWith("--team="))
      .headOption.map(_.split("=", 2)(1))

    //TODO: need a team filter

    // Get year and then conference

    val dir_segments = in_dir.split("/").toList
    val (conference, year) = dir_segments.takeRight(2) match {
      case s1 :: s2 :: Nil if Try(s2.toInt).isSuccess => (s1, s2.toInt)
      case _ => throw new Exception("--in needs to end <<path>>/:conference/:year")
    }

    println("Starting cbb-explorer (BuildLineups):")

    val ncaa_lineup_controller = new LineupController()
    val storage_controller = new StorageController()

    // Iterate over directories
    val subdirs = ls! Path(in_dir)
    val all_games = subdirs.flatMap { subdir =>
      //TODO: add some error validation
      val get_team_id = "(.*)_([0-9]+)$".r
      subdir.last match {
        case get_team_id(team_name, team_id)
          if maybe_team_selector.forall(sel => team_name.contains(sel))
        =>
          val team_dir =  subdir/ "stats.ncaa.org"
          val decoded_team_name = URLDecoder.decode(team_name.replace("+", " "))
          ncaa_lineup_controller.build_team_lineups(
            team_dir, TeamId(decoded_team_name),
            min_time_filter = maybe_filter.map(_*1000) //(convert to ms)
          )

        case get_team_id(team_name, _) =>
          println(s"Skipping unselected team with dir ${subdir.toString}")
          List()

        case _ =>
          println(s"Skipping unrecognized dir ${subdir.toString}")
          List()
      }
    }
    val time_filter_suffix = maybe_filter.map("_" + _).getOrElse("")
    storage_controller.write_lineups(
      lineups = all_games.map(l => strip_unused_data match {
        case true => l.copy(
          players_in = Nil,
          players_out = Nil,
          raw_game_events = Nil
        )
        case false => l
      }).toList,
      file_root = Path(out_dir),
      file_name = s"${conference}_$year${time_filter_suffix}.ndjson"
    )
  }
}
