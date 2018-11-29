package org.piggottfamily.cbb_explorer.models.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.joda.time.DateTime

/**
 * Represents a portion of a game in which a given lineup occurs
 * @param date The date of the game
 * @param start_min The point in the game at which the lineup entered
 * @param end_min The point in the game at which the lineup changed
 * @param duration_mins The duration of the lineup
 * @param score_diff The score differential when the lineup entered
 * @param team The team under analysis
 * @param opponent The opposing team
 * @param lineup_id A string that defines the set of players on the floor (via per player codes)
 * @param players A mapping from the player code used in the lineup to the full name
 * @param raw_team_events A list of the event strings from the NCAA website for the team under analysis
 * @param raw_opponent_events A list of the event strings for the opposting teams
 * @param team_stats The numerical stats extracted for the lineup (for the team under analysis)
 * @param opponent_stats The numerical stats extracted for the lineup (for the opposing team)
 */
case class LineupEvent(
  date: DateTime,
  start_min: Double,
  end_min: Double,
  duration_mins: Double,
  score_diff: Int,
  team: TeamSeasonId,
  opponent: TeamSeasonId,
  lineup_id: LineupEvent.LineupId,
  players: List[LineupEvent.PlayerCodeId],
  players_in: List[LineupEvent.PlayerCodeId],
  players_out: List[LineupEvent.PlayerCodeId],
  raw_team_events: List[String],
  raw_opponent_events: List[String],
  team_stats: LineupEventStats,
  opponent_stats: LineupEventStats
)

object LineupEvent {

  /** A string that defines the set of players on the floor (via per player codes) */
  case class LineupId(value: String) extends AnyVal

  object LineupId {
    /** We have to calculate lineup ids at the end of the event, this is a placeholder until then */
    val unknown = LineupId("")
  }

  /** The full player name together with the code that is unique within the team/season only */
  case class PlayerCodeId(code: String, id: PlayerId)
}
