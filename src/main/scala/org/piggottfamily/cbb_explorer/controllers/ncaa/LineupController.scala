package org.piggottfamily.cbb_explorer.controllers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa._

import LineupController._

import ammonite.ops._
import scala.util.matching.Regex
import scala.util.{Try, Success, Failure}

/** Top level business logic for parsing the different datasets */
class LineupController(d: Dependencies = Dependencies())
{
  /** Builds up a list of a team's good lineups (logging and returning errored lineups) */
  def build_team_lineups(
    root_dir: Path, team: TeamId,
    game_id_filter: Option[Regex] = None, min_time_filter: Option[Long] = None
  )
  : (List[LineupEvent], List[LineupEvent], List[PlayerEvent]) =
  {
    sealed trait LineupError
    case class FileError(f: Path, ex: Throwable) extends LineupError
    case class ParserError(f: Path, l: List[ParseError]) extends LineupError

    // Get neutral game dates
    val neutral_games = (for {
      team_filename <- d.file_manager.list_files(root_dir / teams_dir, Some("html")).iterator
      team_html = d.file_manager.read_file(team_filename)

      results <- TeamScheduleParser.get_neutral_games(team_filename.last, team_html) match {
        case Left(error) =>
          d.logger.info(s"Failed to parse neutral games: [$error]")
          None //(carry on)
        case Right((checked_team, neutral_set)) if checked_team == team =>
          d.logger.info(s"Neutral game dates: [$neutral_set]")
          Some(neutral_set)
        case _ => //(not the right team, ignore)
          None
      }
    } yield results).take(1).toList.headOption.getOrElse {
      d.logger.info(s"Failed to find any neutral games")
      Set[String]()
    }

    // Get lineups for selected games

    val file_filter = min_time_filter.map { min_time =>
      (file_time: Long) => {
        file_time > min_time
      }
    }

    val lineups = for {
      game <- d.file_manager.list_files(root_dir / play_by_play_dir, Some("html"), file_filter).iterator

      game_id = game.last.split("[.]")(0)
      if game_id_filter.forall(_.findFirstIn(game_id).isDefined)

      _ = d.logger.info(s"Reading [$game]: [$game_id]")

      lineup = Try { build_game_lineups(root_dir, game_id, team, neutral_games) } match {
        case Success(res) => res.left.map { errs => ParserError(game, errs) }
        case Failure(ex) => Left(FileError(game, ex))
      }
    } yield lineup //(good, bad, player_event) triples

    case class State(
      good_lineups: List[LineupEvent], bad_lineups: List[LineupEvent], player_events: List[PlayerEvent]
    )
    val end_state = lineups.foldLeft(State(Nil, Nil, Nil)) { (state, lineup_info) =>
      lineup_info match {
        case Right((good, bad, player)) =>
          d.logger.info(s"Successful parse: good=[${good.size}] bad=[${bad.size}]")
          state.copy(
            good_lineups = state.good_lineups ++ good,
            bad_lineups = state.bad_lineups ++ bad,
            player_events = state.player_events ++ player
          )
        case Left(FileError(game, ex)) =>
          d.logger.info(s"File error with [$game]: [$ex]")
          state
        case Left(ParserError(game, errors)) =>
          d.logger.info(s"Parse error with [$game]: [$errors]")
          state
      }
    }
    (end_state.good_lineups, end_state.bad_lineups, end_state.player_events)
  }

  /** Given a game/team id, returns good and bad paths for that game */
  def build_game_lineups(root_dir: Path, game_id: String, team: TeamId, neutral_game_dates: Set[String]):
    Either[List[ParseError], (List[LineupEvent], List[LineupEvent], List[PlayerEvent])] =
  {
    val playbyplay_filename = s"$game_id.html"
    val boxscore_filename = s"${game_id}42b2.html" //(encoding of 1st period box score)
    val box_html = d.file_manager.read_file(root_dir / boxscore_dir / boxscore_filename)
    val play_by_play_html = d.file_manager.read_file(root_dir / play_by_play_dir / playbyplay_filename)
    for {
      box_lineup <- d.boxscore_parser.get_box_lineup(boxscore_filename, box_html, team, neutral_game_dates)
      _ = d.logger.info(s"Parsed box score: opponent=[${box_lineup.opponent}] venue=[${box_lineup.location_type}]")
      lineup_events <- d.playbyplay_parser.create_lineup_data(playbyplay_filename, play_by_play_html, box_lineup)
      player_events = (lineup_events._1 ++ lineup_events._2).flatMap(
        //(note we are including bad lineups in our player events since it's not such a disaster -
        // what we care about is mostly the individual stats)
        LineupUtils.create_player_events(_, box_lineup)
      )
    } yield (lineup_events._1, lineup_events._2, player_events)
  }

}

object LineupController {

  val teams_dir = RelPath("teams")
  val play_by_play_dir = RelPath("game") / RelPath("play_by_play")
  val boxscore_dir = RelPath("game") / RelPath("box_score")

  /** Dependency injection */
  case class Dependencies(
    boxscore_parser: BoxscoreParser = BoxscoreParser,
    playbyplay_parser: PlayByPlayParser = PlayByPlayParser,
    logger: LogUtils = LogUtils,
    file_manager: FileUtils = FileUtils
  )
}
