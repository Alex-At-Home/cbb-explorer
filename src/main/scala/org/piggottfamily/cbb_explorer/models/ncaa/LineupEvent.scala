package org.piggottfamily.cbb_explorer.models.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.joda.time.DateTime

/** Represents a portion of a game in which a given lineup occurs
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
    opponent_stats: LineupEventStats,
    player_count_error: Option[Int] = None
)

object LineupEvent {

  /** List of game events, categorized by whether it was "for" the team or its
    * opponent
    */
  case class RawGameEvent(
      min: Double,
      /** The format is date,time,event */
      team: Option[String] = None,
      /** The format is date,time,event */
      opponent: Option[String] = None
  ) {

    /** Gets the event information (either from team or opponent - can't be
      * both)
      */
    def get_info: Option[String] = team.orElse(opponent)

    /** Gets the event information (either from team or opponent - can't be
      * both)
      */
    def info: String = get_info.getOrElse("")

    /** For display - shows the direction of the event (team > or opponent <) */
    def show_dir: String = team.map(_ => ">").getOrElse("<")

    /** Gets the date string associated with the event */
    def get_date_str: Option[String] = get_info.map { ev_str =>
      ev_str.split(',')(0)
    }

    /** Gets the date string associated with the event */
    def date_str: String = get_date_str.getOrElse("")

    /** Gets the score string associated with the event */
    def get_score_str: Option[String] = get_info.flatMap { ev_str =>
      ev_str.split(',') match {
        case a if a.size > 1 => Some(a(1))
        case _               => None
      }
    }

    /** Gets the date score associated with the event */
    def score_str: String = get_score_str.getOrElse("0-0")

  }
  object RawGameEvent {
    def team(s: String, min: Double): RawGameEvent =
      RawGameEvent(min, Some(s), None)
    def opponent(s: String, min: Double): RawGameEvent =
      RawGameEvent(min, None, Some(s))
    object Team {
      def unapply(x: RawGameEvent): Option[String] = x.team
    }
    object Opponent {
      def unapply(x: RawGameEvent): Option[String] = x.opponent
    }

    /** Which team is in possession */
    object Direction extends Enumeration {
      val Init, Team, Opponent = Value
    }

    /** Utility for decomposing game events into offensive and defensive
      * possessions
      */
    case class PossessionEvent(dir: Direction.Value) {

      /** The team in possession */
      object AttackingTeam {
        def unapply(x: RawGameEvent): Option[String] = x match {
          case RawGameEvent.Team(event_str) if dir == Direction.Team =>
            Some(event_str)
          case RawGameEvent.Opponent(event_str) if dir == Direction.Opponent =>
            Some(event_str)
          case _ => None
        }
      }

      /** The team not in possession */
      object DefendingTeam {
        def unapply(x: RawGameEvent): Option[String] = x match {
          case RawGameEvent.Team(event_str) if dir == Direction.Opponent =>
            Some(event_str)
          case RawGameEvent.Opponent(event_str) if dir == Direction.Team =>
            Some(event_str)
          case _ => None
        }
      }
    }
  }

  /** Info about the score at the start and end of the event */
  case class ScoreInfo(
      start: Game.Score,
      end: Game.Score,
      start_diff: Int,
      end_diff: Int
  )

  object ScoreInfo {
    def empty: ScoreInfo = ScoreInfo(
      Game.Score(0, 0),
      Game.Score(0, 0),
      0,
      0
    )
  }

  /** A string that defines the set of players on the floor (via per player
    * codes)
    */
  case class LineupId(value: String) extends AnyVal

  object LineupId {

    /** We have to calculate lineup ids at the end of the event, this is a
      * placeholder until then
      */
    val unknown = LineupId("")
  }

  /** The full player name together with the code that is unique within the
    * team/season only
    */
  case class PlayerCodeId(
      code: String,
      id: PlayerId,
      ncaa_id: Option[String] = None
  )
}
