package org.piggottfamily.cbb_explorer.controllers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa._

import LineupController._

import ammonite.ops._
import scala.util.matching.Regex

/** Top level business logic for parsing the different datasets */
class LineupController(d: Dependencies = Dependencies())
{
//  def build_team_lineups(root_dir: String):

  /** Given a game id, returns good and bad paths for that game */
  def build_game_lineups(root_dir: Path, game_id: String, team: TeamId):
    Either[List[ParseError], (List[LineupEvent], List[LineupEvent])] =
  {
    val filename = s"$game_id.html"
    val box_html = d.file_manager.read_file(root_dir / boxscore_dir / filename)
    val play_by_play_html = d.file_manager.read_file(root_dir / play_by_play_dir / filename)
    for {
      box_lineup <- d.boxscore_parser.get_box_lineup(filename, box_html, team)
      events <- d.playbyplay_parser.create_lineup_data(filename, play_by_play_html, box_lineup)
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
