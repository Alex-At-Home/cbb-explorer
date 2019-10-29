package org.piggottfamily.cbb_explorer.models.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.joda.time.DateTime

/**
 * Represents the activity of one player within a lineup event
 * @param player_id The specific player whose production is represented
 * @param date The date of the game
 * @param start_min The point in the game at which the lineup entered
 * @param end_min The point in the game at which the lineup changed
 * @param duration_mins The duration of the lineup
 * @param score_diff The score differential when the lineup entered
 * @param team The team under analysis
 * @param opponent The opposing team
 * @param lineup_id A string that defines the set of players on the floor (via per player codes)
 * @param players A mapping from the player code used in the lineup to the full name
 * @param player_stats The countable stats for the player under analysis
 * @param team_stats The numerical stats extracted for the lineup (for the team under analysis)
 * @param opponent_stats The numerical stats extracted for the lineup (for the opposing team)
 */
case class PlayerSummaryEvent(
  player_id: LineupEvent.PlayerCodeId,
  date: DateTime,
  location_type: Game.LocationType.Value,
  start_min: Double,
  end_min: Double,
  duration_mins: Double,
  score_info: LineupEvent.ScoreInfo,
  team: TeamSeasonId,
  opponent: TeamSeasonId,
  lineup_id: LineupEvent.LineupId,
  players: List[LineupEvent.PlayerCodeId],
  player_stats: LineupEventStats,
  team_stats: LineupEventStats,
  opponent_stats: LineupEventStats
)
