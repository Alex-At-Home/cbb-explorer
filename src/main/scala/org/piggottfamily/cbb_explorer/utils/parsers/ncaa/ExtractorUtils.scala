package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._

object ExtractorUtils {

  //TODO: split on timeouts? (and have is_after_timeout flag, or sub_event == in-game/break/timeout)

  //TODO: you _can_ get
  /*
  12:00:00	Jacob Cushing, substitution in	16-17
12:00:00	Darian Bryant, substitution out	16-17
12:00:00	Eric Carter, substitution out	16-17
12:00:00	Collin Goss, substitution in	16-17
12:00:00	Kevin Anderson, substitution out	16-17
12:00:00		16-17	Jalen Smith, substitution out
12:00:00		16-17	Serrel Smith Jr., substitution in
12:00:00		16-17	Darryl Morsell, substitution out
12:00:00		16-17	Bruno Fernando, substitution in
12:00:00		16-17	Aaron Wiggins, substitution in
12:00:00	Ryan Johnson, substitution in	16-17
12:00:00	Team, rebound defensive team	16-17
12:00:00		16-17	Anthony Cowan, substitution out
*/

//TODO: also ... free throws:
/*

17:09		5-1	CARTER JR.,JOHN made Free Throw
17:09		5-1	CARTER JR.,JOHN missed Free Throw

17:09	,RICKY LINDO JR Defensive Rebound	5-1
17:09	,RICKY LINDO JR Enters Game	5-1
*/

//TODO: so we need to retrieve "reorder and reverse" handling:
// - subs in the middle of plays
// - plays in the middle of subs !!

  /** Error enrichment placeholder */
  val `parent_fills_in` = ""

  // Top level

  /** Converts a stream of partially parsed events into a list of lineup events
   * (note box_lineup has all players, but the top 5 are always the starters)
   */
  def build_partial_lineup_list(
    reversed_partial_events: Iterator[Model.PlayByPlayEvent],
    box_lineup: LineupEvent
  ): List[LineupEvent] = {
    val starters_only = box_lineup.copy(players = box_lineup.players.take(5))
    // Use this to render player names in their more readable format
    val all_players_map = box_lineup.players.map(p => p.code -> p.id.name).toMap

    val starting_state = Model.LineupBuildingState(starters_only, Nil)
    val partial_events = reversed_partial_events.toList.reverse
    val end_state = partial_events.foldLeft(starting_state) { (state, event) =>
      def tidy_player(p: String): String =
        all_players_map
          .get(build_player_code(p).code)
          .getOrElse(p)

      event match {
        case Model.SubInEvent(min, player_name) if state.is_active(min) =>
          val tidier_player_name = tidy_player(player_name)
          val completed_curr = complete_lineup(state.curr, min)
          state.copy(
            curr = new_lineup_event(
              completed_curr, in = Some(tidier_player_name)
            ),
            prev = completed_curr :: state.prev
          )
        case Model.SubOutEvent(min, player_name) if state.is_active(min) =>
          val tidier_player_name = tidy_player(player_name)
          val completed_curr = complete_lineup(state.curr, min)
          state.copy(
            curr = new_lineup_event(
              completed_curr, out = Some(tidier_player_name)
            ),
            prev = completed_curr :: state.prev
          )
        case Model.SubInEvent(min, player_name) => // !state.is_active
            // Keep adding sub events
            val tidier_player_name = tidy_player(player_name)
            state.with_player_in(tidier_player_name)

        case Model.SubOutEvent(min, player_name) => // !state.is_active
          // Keep adding sub events
          val tidier_player_name = tidy_player(player_name)
          state.with_player_out(tidier_player_name)

        case Model.OtherTeamEvent(min, score, event_string) =>
          state.with_team_event(min, event_string).with_latest_score(score)

        case Model.OtherOpponentEvent(min, score, event_string) =>
          state.with_opponent_event(min, event_string).with_latest_score(score)

        case Model.GameBreakEvent(min) =>
          val completed_curr = complete_lineup(state.curr, min)
          state.copy(
            curr = new_lineup_event(completed_curr).copy(
              lineup_id = starters_only.lineup_id,
              players = starters_only.players //reset lineup
            ),
            prev = completed_curr :: state.prev
          )
        case Model.GameEndEvent(min) =>
          state.copy(curr = complete_lineup(state.curr, min))
      }
    }
    end_state.build()
  }

  // Utils with some exernal usefulness

  /** Pulls team name from "title" table element, matching the target and opponent teams
    * returns the target team, the opposing team, and whether the target team is first (vs second)
  */
  def parse_team_name(teams: List[String], target_team: TeamId)
    : Either[ParseError, (String, String, Boolean)] =
  {
    val target_team_str = target_team.name
    teams.map(_.trim) match {
      case List(`target_team_str`, opponent) =>
        Right((target_team_str, opponent, true))

      case List(opponent, `target_team_str`) =>
        Right((target_team_str, opponent, false))

      case _ =>
      Left(ParseUtils.build_sub_error("team")(
        s"Could not find/match team names (target=[$target_team]): $teams"
      ))
    }
  }

  /** Pulls out inconsistent lineups (self healing seems harder
    * based on cases I've seen, eg
    * player B enters game+player A leaves game ...
    * ... A makes shot...player A enters game)
   */
  def validate_lineup(
    lineup_event: LineupEvent
  ): Boolean = {
    val right_number_of_players = lineup_event.players.size == 5
    // We also see cases where players not in a lineup make plays
    //TODO we're going to ignore those for the moment
    right_number_of_players
  }

  /** Gets the start time from the period - ie 2x 20 minute halves, then 5m overtimes */
  def start_time_from_period(period: Int): Double = (period - 1) match {
    case n if n < 2 => n*20.0
    case m => 40.0 + (m - 2)*5.0
  }
  /** Gets the end time (ie game duration to date) from the period
  *   - ie 2x 20 minute halves, then 5m overtimes
  */
  def duration_from_period(period: Int): Double = start_time_from_period(period + 1)

  /** Builds a player code out of the name, with various formats supported */
  def build_player_code(name: String): LineupEvent.PlayerCodeId = {
    LineupEvent.PlayerCodeId((name.split("\\s*,\\s*", 2).toList match {
      case all_name_set :: Nil =>
        all_name_set.split("\\s+").toList
      case last_name_set :: first_name_set :: Nil =>
        first_name_set.split("\\s+").toList ++ last_name_set.split("\\s+").toList
      case _ => Nil //(impossible by construction of split)
    }).map {
      _.toLowerCase
    }.filterNot { candidate => // get rid or jr/sr/ii/etc
      candidate.size < 2 ||
      candidate(0).isDigit ||
      candidate == "the" ||
      candidate == "first" || candidate == "second" || candidate == "third" ||
      candidate == "jr" || candidate == "jr." ||
      candidate == "sr" || candidate == "sr." ||
        (candidate.startsWith("ii") &&
          (candidate.endsWith("ii") || candidate.endsWith("i."))
        )
    }.map { transform =>
      s"${transform(0).toUpper}${transform(1).toLower}"
    }.mkString(""), PlayerId(name))
  }

  // Internal Utils

  /** Builds a lineup id from a list of players */
  private def build_lineup_id(players: List[LineupEvent.PlayerCodeId]): LineupEvent.LineupId = {
    LineupEvent.LineupId(players.map(_.code).sorted.mkString("_"))
  }

  /** Creates an "empty" new lineup - note "prev" has had "complete_lineup" called on it */
  private def new_lineup_event(
    prev: LineupEvent,
    in: Option[String] = None, out: Option[String] = None
  ): LineupEvent = {
    LineupEvent(
      date = prev.date.plusMillis((prev.duration_mins*60000.0).toInt),
      start_min = prev.end_min,
      end_min = prev.end_min, //(updates with every event)
      duration_mins = 0.0, //(fill in at end of event)
      LineupEvent.ScoreInfo.empty.copy(
        start = prev.score_info.end,
        end = prev.score_info.end,
        start_diff = prev.score_info.end_diff
      ), //(complete later)
      team = prev.team,
      opponent = prev.opponent,
      lineup_id = LineupEvent.LineupId.unknown, //(will calc once we have all the subs)
      players = prev.players, //(will re-calc once we have all the subs)
      players_in = in.map(build_player_code).toList,
      players_out = out.map(build_player_code).toList,
      raw_game_events = Nil,
      team_stats = LineupEventStats.empty, //(calculate these 2 later)
      opponent_stats = LineupEventStats.empty
    )
  }

  /** Fills in/tidies up a partial lineup event following its completion */
  private def complete_lineup(curr: LineupEvent, min: Double): LineupEvent = {
    val curr_players = curr.players.map(p => p.code -> p).toMap
    val curr_players_out = curr.players_out.map(p => p.code -> p).toMap
    val curr_players_in = curr.players_in.map(p => p.code -> p).toMap
    val new_player_list =
      (curr_players -- curr_players_out.keySet ++ curr_players_in).values.toList

    curr.copy(
      end_min = min,
      duration_mins = min - curr.start_min,
      score_info = curr.score_info.copy(
        end_diff = curr.score_info.end.scored - curr.score_info.end.allowed
      ),
      lineup_id = build_lineup_id(new_player_list),
      players = new_player_list.sortBy(_.code),
      raw_game_events = curr.raw_game_events.reverse
    )
  }

  // Models (used by the parser also)

  object Model {
    private val SUB_SAFETY_DELTA_MINS = 4.0/60 //4s

    /* State for building raw line-up data */
    private [ExtractorUtils] case class LineupBuildingState(
      curr: LineupEvent,
      prev: List[LineupEvent]
    ) {
      def build(): List[LineupEvent] = {
        (curr :: prev).reverse
      }
      /** Opposition subs are currently treated as game events. but shouldn't
       *  result in new lineups */
      private def is_sub(raw: LineupEvent.RawGameEvent): Boolean = raw.opponent.map { s =>
        val s_lower = s.toLowerCase.trim
        //TODO: move this into some parsing module
        s_lower.endsWith("leaves game") || s_lower.endsWith("enters game") ||
        s_lower.endsWith("substitution in") || s_lower.endsWith("substitution out")
      }.getOrElse(false)

      /** Ifsome time has elapsed since the last sub or a game event has occurred */
      def is_active(min: Double): Boolean =
        curr.raw_game_events.filterNot(is_sub).nonEmpty ||
        {
          min - curr.end_min > SUB_SAFETY_DELTA_MINS
        }

      // State manipulation

      def with_player_in(player_name: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            players_in = build_player_code(player_name) :: curr.players_in
          )
        )
      def with_player_out(player_name: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            players_out = build_player_code(player_name) :: curr.players_out
          )
        )
      def with_latest_score(score: Game.Score): LineupBuildingState = {
        copy(
          curr = curr.copy(
            score_info = curr.score_info.copy(
              end = score
            )
          )
        )
      }
      def with_team_event(min: Double, event_string: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            end_min = min,
            raw_game_events = LineupEvent.RawGameEvent.team(event_string) :: curr.raw_game_events
          )
        )
      def with_opponent_event(min: Double, event_string: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            end_min = min,
            raw_game_events = LineupEvent.RawGameEvent.opponent(event_string) :: curr.raw_game_events
          )
        )
    }

    sealed trait PlayByPlayEvent {
      def min: Double
      def with_min(new_min: Double): PlayByPlayEvent
    }
    case class SubInEvent(min: Double, player_name: String) extends PlayByPlayEvent {
      def with_min(new_min: Double): SubInEvent = copy(min = new_min)
    }
    case class SubOutEvent(min: Double, player_name: String) extends PlayByPlayEvent {
      def with_min(new_min: Double): SubOutEvent = copy(min = new_min)
    }
    case class OtherTeamEvent(min: Double, score: Game.Score, event_string: String) extends PlayByPlayEvent {
      def with_min(new_min: Double): OtherTeamEvent = copy(min = new_min)
    }
    case class OtherOpponentEvent(min: Double, score: Game.Score, event_string: String) extends PlayByPlayEvent {
      def with_min(new_min: Double): OtherOpponentEvent = copy(min = new_min)
    }
    case class GameBreakEvent(min: Double) extends PlayByPlayEvent {
      def with_min(new_min: Double): GameBreakEvent = copy(min = new_min)
    }
    case class GameEndEvent(min: Double) extends PlayByPlayEvent {
      def with_min(new_min: Double): GameEndEvent = copy(min = new_min)
    }
  }
}
