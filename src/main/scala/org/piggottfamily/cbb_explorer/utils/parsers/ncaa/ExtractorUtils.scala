package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object ExtractorUtils {

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
    }.filter { candidate => // get rid or jr/sr/ii/etc
      candidate.size < 2 ||
      candidate(0).isDigit ||
      candidate == "first" || candidate == "second" || candidate == "third" ||
      candidate == "jr" || candidate == "jr." ||
      candidate == "sr" || candidate == "sr." ||
        (candidate.startsWith("ii") &&
          (candidate.endsWith("ii") || candidate.endsWith("i."))
        )
    }.map { transform =>
      transform(0).toUpper + transform(1).toLower
    }.mkString(""), PlayerId(name))
  }

  /** Builds a lineup id from a list of players */
  private def build_lineup_id(players: List[LineupEvent.PlayerCodeId]): LineupEvent.LineupId = {
    LineupEvent.LineupId(players.map(_.code).sorted.mkString("/"))
  }

  /** Orders play-by-play data to ensure that all the subs are at the start */
  private def reorder_and_reverse(
    reversed_partial_events: Iterator[Model.PlayByPlayEvent]
  ): List[Model.PlayByPlayEvent] = {
    /** Ensures all the sub data is at the start of the block */
    def inner_sort(in: List[Model.PlayByPlayEvent]): List[Model.PlayByPlayEvent] = {
      in.zipWithIndex.sortBy {
        case (_: Model.SubInEvent, index) => -index
        case (_: Model.SubOutEvent, index) => -index
        case (_, index) => index
      }.map(_._1)
    }

    val starting_state = List[List[Model.PlayByPlayEvent]]()
    (reversed_partial_events.foldLeft(starting_state) { (acc, event) => acc match {
      case Nil =>
        // Create a new block of play by play events
        (event :: Nil) :: Nil
      case Nil :: tail =>
        // Create a new block of play by play events (in practice this won't happen)
        (event :: Nil) :: tail
      case (head :: inner_tail) :: outer_tail if head.min == event.min =>
        // Add the new play by play to the existing block
        (event :: head :: inner_tail) :: outer_tail
      case (curr@(head :: inner_tail)) :: outer_tail => //head.min != event.min
        // Reorder the existing block then add a new one
        (event :: Nil) :: inner_sort(curr) :: outer_tail

    }}).flatten
  }

  /** Creates an "empty" new lineup */
  private def new_lineup_event(
    min: Double, prev: LineupEvent,
    in: Option[String] = None, out: Option[String] = None
  ): LineupEvent = {
    LineupEvent(
      date = prev.date,
      start_min = min,
      end_min = min, //(updates with every event)
      duration_mins = 0.0, //(fill in at end of event)
      score_diff = 0, //(calculate later)
      team = prev.team,
      opponent = prev.opponent,
      lineup_id = LineupEvent.LineupId.unknown, //(will calc once we have all the subs)
      players = prev.players, //(will re-calc once we have all the subs)
      players_in = in.map(build_player_code).toList,
      players_out = out.map(build_player_code).toList,
      raw_team_events = Nil,
      raw_opponent_events = Nil,
      team_stats = LineupEventStats.empty, //(calculate these 2 later)
      opponent_stats = LineupEventStats.empty
    )
  }

  /** Fills in/tidies up a partial lineup event following its completion */
  private def complete_lineup(curr: LineupEvent, min: Double): LineupEvent = {
    val new_player_list =
      (curr.players.toSet -- curr.players_out.toSet ++ curr.players_in.toSet).toList

    curr.copy(
      end_min = min,
      duration_mins = min - curr.start_min,
      lineup_id = build_lineup_id(new_player_list),
      players = new_player_list,
      raw_team_events = curr.raw_team_events.reverse,
      raw_opponent_events = curr.raw_opponent_events.reverse
    )
  }

  /** Converts a stream of partially parsed events into a list of lineup events */
  def build_partial_lineup_list(
    reversed_partial_events: Iterator[Model.PlayByPlayEvent],
    starting_lineup: LineupEvent
  ): List[LineupEvent] = {

    val starting_state = Model.LineupBuildingState(starting_lineup, Nil)
    val partial_events = reorder_and_reverse(reversed_partial_events)
    val end_state = partial_events.foldLeft(starting_state) { (state, event) =>
      event match {
        case Model.SubInEvent(min, player_name) if state.is_active(min) =>
          state.copy(
            curr = new_lineup_event(
              min, state.curr, in = Some(player_name)
            ),
            prev = complete_lineup(state.curr, min) :: state.prev
          )
        case Model.SubOutEvent(min, player_name) if state.is_active(min) =>
          state.copy(
            curr = new_lineup_event(
              min, state.curr, out = Some(player_name)
            ),
            prev = complete_lineup(state.curr, min) :: state.prev
          )
        case Model.SubInEvent(min, player_name) => // !state.isActive
            // Keep adding sub events
            state.with_player_in(player_name)

        case Model.SubOutEvent(min, player_name) => // !state.isActive
          // Keep adding sub events
          state.with_player_out(player_name)

        case Model.OtherTeamEvent(min, event_string) =>
          state.with_team_event(min, event_string)

        case Model.OtherOpponentEvent(min, event_string) =>
          state.with_opponent_event(min, event_string)

        case Model.GameBreakEvent(min, event_string, next_lineup) =>
          state.copy(
            curr = next_lineup,
            prev = complete_lineup(state.curr, min) :: state.prev
          )
        case Model.GameEndEvent(min) =>
          state.copy(curr = complete_lineup(state.curr, min))
      }
    }
    end_state.build()
  }

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
      /** Ifsome time has elapsed since the last sub or a game event has occurred */
      def is_active(min: Double): Boolean =
        curr.raw_team_events.nonEmpty || curr.raw_opponent_events.nonEmpty ||
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
      def with_team_event(min: Double, event_string: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            end_min = min,
            raw_team_events = event_string :: curr.raw_team_events
          )
        )
      def with_opponent_event(min: Double, event_string: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            end_min = min,
            raw_opponent_events = event_string :: curr.raw_opponent_events
          )
        )
    }

    sealed trait PlayByPlayEvent {
      def min: Double
    }
    case class SubInEvent(min: Double, player_name: String) extends PlayByPlayEvent
    case class SubOutEvent(min: Double, player_name: String) extends PlayByPlayEvent
    case class OtherTeamEvent(min: Double, event_string: String) extends PlayByPlayEvent
    case class OtherOpponentEvent(min: Double, event_string: String) extends PlayByPlayEvent
    case class GameBreakEvent(
      min: Double, event_string: String, next_lineup: LineupEvent
    ) extends PlayByPlayEvent
    case class GameEndEvent(min: Double) extends PlayByPlayEvent
  }
}
