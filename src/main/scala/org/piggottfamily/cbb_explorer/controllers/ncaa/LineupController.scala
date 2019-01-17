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
  /** Builds up a list of a team's good lineups (logging but otherwise ignoring errors) */
  def build_team_lineups(root_dir: Path, team: TeamId): List[LineupEvent] = {
    sealed trait LineupError
    case class FileError(f: Path, ex: Throwable) extends LineupError
    case class ParserError(f: Path, l: List[ParseError]) extends LineupError

    val lineups = for {
      game <- d.file_manager.list_files(root_dir / play_by_play_dir, Some("html")).iterator

      _ = d.logger.info(s"Reading [$game]")
      game_id = game.last.split("[.]")(0)
      _ = d.logger.info(s"Reading [$game_id]")

      lineup = Try { build_game_lineups(root_dir, game_id, team) } match {
        case Success(res) => res.left.map { errs => ParserError(game, errs) }
        case Failure(ex) => Left(FileError(game, ex))
      }
    } yield lineup

    case class State(good_lineups: List[LineupEvent], num_bad_lineups: Int)
    val end_state = lineups.foldLeft(State(Nil, 0)) { (state, lineup_info) =>
      lineup_info match {
        case Right((good, bad)) =>
          d.logger.info(s"Successful parse: good=[${good.size}] bad=[${bad.size}]")
          state.copy(
            good_lineups = state.good_lineups ++ good,
            num_bad_lineups = state.num_bad_lineups + bad.size
          )
        case Left(FileError(game, ex)) =>
          d.logger.info(s"File error with [$game]: [$ex]")
          state
        case Left(ParserError(game, errors)) =>
          d.logger.info(s"Parse error with [$game]: [$errors]")
          state
      }
    }
    end_state.good_lineups
  }

  /** Given a game/team id, returns good and bad paths for that game */
  def build_game_lineups(root_dir: Path, game_id: String, team: TeamId):
    Either[List[ParseError], (List[LineupEvent], List[LineupEvent])] =
  {
    val playbyplay_filename = s"$game_id.html"
    val boxcore_filename = s"${game_id}42b2.html" //(encoding of 1st period box score)
    val box_html = d.file_manager.read_file(root_dir / boxscore_dir / boxcore_filename)
    val play_by_play_html = d.file_manager.read_file(root_dir / play_by_play_dir / playbyplay_filename)
    for {
      box_lineup <- d.boxscore_parser.get_box_lineup(boxcore_filename, box_html, team)
      events <- d.playbyplay_parser.create_lineup_data(playbyplay_filename, play_by_play_html, box_lineup)
    } yield events
  }

}

object LineupController {

  val play_by_play_dir = RelPath("play_by_play")
  val boxscore_dir = RelPath("box_score")

  /** Dependency injection */
  case class Dependencies(
    boxscore_parser: BoxscoreParser = BoxscoreParser,
    playbyplay_parser: PlayByPlayParser = PlayByPlayParser,
    logger: LogUtils = LogUtils,
    file_manager: FileUtils = FileUtils
  )
}
