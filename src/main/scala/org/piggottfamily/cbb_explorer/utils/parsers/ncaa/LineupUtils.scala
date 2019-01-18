package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object LineupUtils {
  import ExtractorUtils._

  /** TODO build the stats from the game events */
  def enrich_lineup(lineup: LineupEvent): LineupEvent = {
    val scored = lineup.score_info.end.scored - lineup.score_info.start.scored
    val allowed = lineup.score_info.end.allowed - lineup.score_info.start.allowed
    lineup.copy(
      team_stats = lineup.team_stats.copy(
        num_events = lineup.raw_game_events.filter(_.team.isDefined).size,
        pts = scored,
        plus_minus = scored - allowed
      ),
      opponent_stats = lineup.opponent_stats.copy(
        num_events = lineup.raw_game_events.filter(_.opponent.isDefined).size, //TODO exclude subs
        pts = allowed,
        plus_minus = allowed - scored
      )
    )
  }
}
