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
  team_stats: LineupEventStats,
  opponent_stats: LineupEventStats
)

object LineupEvent {

  /** List of game events, categorized by whether it was "for" the team or its opponent */
  case class RawGameEvent(
    /** The format is date,time,event*/
    team: Option[String] = None,
    /** The format is date,time,event*/
    opponent: Option[String] = None,
    team_possession: Option[Int] = None,
    opponent_possession: Option[Int] = None
  ) {
    /** Gets the date string associated with the event */
    def get_date_str: String = team.orElse(opponent).map { ev_str =>
      ev_str.split(",", 1)(0)
    }.getOrElse("")
  }
  object RawGameEvent {
    def team(s: String): RawGameEvent = RawGameEvent(Some(s), None)
    def opponent(s: String): RawGameEvent = RawGameEvent(None, Some(s))
    object Team {
      def unapply(x: RawGameEvent): Option[String] = x.team
    }
    object Opponent {
      def unapply(x: RawGameEvent): Option[String] = x.opponent
    }
  }

  /** Info about the score at the start and end of the event */
  case class ScoreInfo(start: Game.Score, end: Game.Score, start_diff: Int, end_diff: Int)

  object ScoreInfo {
    def empty: ScoreInfo = ScoreInfo(
      Game.Score(0, 0), Game.Score(0, 0), 0, 0
    )
  }

  /** A string that defines the set of players on the floor (via per player codes) */
  case class LineupId(value: String) extends AnyVal

  object LineupId {
    /** We have to calculate lineup ids at the end of the event, this is a placeholder until then */
    val unknown = LineupId("")
  }

  /** The full player name together with the code that is unique within the team/season only */
  case class PlayerCodeId(code: String, id: PlayerId)
}
