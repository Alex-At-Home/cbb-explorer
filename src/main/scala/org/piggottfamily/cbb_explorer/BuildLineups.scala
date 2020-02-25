package org.piggottfamily.cbb_explorer

import ammonite.ops._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.ncaa.LineupController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.LineupErrorAnalysisUtils
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
        |[--player-events] (includes player events in a separate file)
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

    val include_player_events =
      !args.toList.map(_.trim).exists(_ == "--player-events")

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
    val (good_games, lineup_errors, player_events) = subdirs.map { subdir =>
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
          (List(), List(), List())

        case _ =>
          println(s"Skipping unrecognized dir ${subdir.toString}")
          (List(), List(), List())
      }
    }.foldLeft((List[LineupEvent](), List[LineupEvent](), List[PlayerEvent]())) {
      case ((all_good, all_bad, all_player), (new_good, new_bad, new_player)) =>
        (all_good ++ new_good, all_bad ++ new_bad, all_player ++ new_player)
    }
    val time_filter_suffix = maybe_filter.map("_" + _).getOrElse("")
    // Write good lineups
    storage_controller.write_lineups(
      lineups = good_games.map(l => strip_unused_data match {
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
    // Write bad lineups
    storage_controller.write_lineups(
      lineups = lineup_errors.map(l => strip_unused_data match {
        case true => l.copy(
          players_in = Nil,
          players_out = Nil,
          raw_game_events = Nil
        )
        case false => l
      }).toList,
      file_root = Path(out_dir),
      file_name = s"bad_lineups_${conference}_$year${time_filter_suffix}.ndjson"
    )
    if (include_player_events) {
      storage_controller.write_player_events(
        player_events = player_events.map(p => strip_unused_data match {
          case true => p.copy(
            players_in = Nil,
            players_out = Nil,
            raw_game_events = Nil
          )
          case false => p
        }).toList,
        file_root = Path(out_dir),
        file_name = s"player_events_${conference}_$year${time_filter_suffix}.ndjson"
      )
    }

    // Add some information about bad lineups:
    println(s"[LineupErrorAnalysis] Total lineup errors (conf=[$conference]) [${lineup_errors.size}] (good: [${good_games.size}])")
    val num_good_possessions = good_games.foldLeft(0) { (acc, lineup) => acc + lineup.team_stats.num_possessions }
    val num_bad_possessions = lineup_errors.foldLeft(0) { (acc, lineup) => acc + lineup.team_stats.num_possessions }
    println(s"[LineupErrorAnalysis] Total possession errors: [$num_bad_possessions] (good: [$num_good_possessions])")
    val bad_lineup_analysis = LineupErrorAnalysisUtils.categorize_bad_lineups(lineup_errors);
    println(s"[LineupErrorAnalysis] Bad lineup analysis: [$bad_lineup_analysis]")
  }
}
