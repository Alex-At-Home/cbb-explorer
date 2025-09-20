package org.piggottfamily.cbb_explorer.models.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa.LineupEvent._
import org.piggottfamily.cbb_explorer.models.ncaa.CutdownShotEvent._
import org.joda.time.DateTime

/** Represents a portion of a game in which a given lineup occurs - within that
  * portion, the stats of an individual
  * @param player
  *   The player in question
  * @param player_stats
  *   The numerical stats extracted for the player (for the team under analysis)
  * @param date
  *   The date of the game
  * @param start_min
  *   The point in the game at which the lineup entered
  * @param end_min
  *   The point in the game at which the lineup changed
  * @param duration_mins
  *   The duration of the lineup
  * @param score_diff
  *   The score differential when the lineup entered
  * @param team
  *   The team under analysis
  * @param opponent
  *   The opposing team
  * @param lineup_id
  *   A string that defines the set of players on the floor (via per player
  *   codes)
  * @param players
  *   A mapping from the player code used in the lineup to the full name
  * @param players_in
  *   The list of players who subbed in for this event
  * @param players_out
  *   The list of players who subbed out for this event
  * @param raw_game_events
  *   A list of the event strings from the NCAA website for the teams under
  *   analysis
  * @param team_stats
  *   The numerical stats extracted for the lineup (for the team under analysis)
  * @param opponent_stats
  *   The numerical stats extracted for the lineup (for the opposing team)
  * @param player_count_error
  *   If a lineup is "impossible", count the number of players in the lineup for
  *   analysis purposes
  */
case class PlayerEvent(
    player: LineupEvent.PlayerCodeId,
    player_stats: LineupEventStats,
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
    players_in: List[LineupEvent.PlayerCodeId],
    players_out: List[LineupEvent.PlayerCodeId],
    raw_game_events: List[LineupEvent.RawGameEvent],
    team_stats: LineupEventStats, // (optional if event_meta specified)
    opponent_stats: LineupEventStats, // (optional if event_meta specified)
    player_count_error: Option[Int] = None
    // Additional fields representing "each lineup is a single event"
    // event_meta: Option[PlayerEvent.SingleEventMeta] = None,
    // game_id: Option[String] = None
)

object PlayerEvent {

  /** For player events that consist of a single event we can provide more info
    * For player stats: aggregate `player_stats` as per usual
    *
    * For team stats: (ie all team stats while playing) .. all player_stats with
    * player filter? (plus a "team" player?) ... and then merge them
    *
    * For opponent stats: same .. player_name can be DEF_(G|W|F)
    */
  case class SingleEventMeta(
      shot_clock: Double,
      min: Double,
      prev_event: Option[String],
      // TODO what are the options here: (Missed|Made)_Mid / (Missed|Made)_3P / (Missed|Made)_Rim
      // "ORB ORB_(3P|Mid|Rim)" / "DRB DRB_(3P|Mid|Rim)" / TOV / ATO
      shot_info: Option[CutdownShotEvent]
  )
}
