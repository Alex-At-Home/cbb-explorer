package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

/** Utilities related to building up the lineup stats */
trait LineupUtils {
  import ExtractorUtils._

  /** TODO build the stats from the game events */
  def enrich_lineup(lineup: LineupEvent, prev_lineup: Option[LineupEvent])
    : (LineupEvent, Option[LineupEvent]) =
  {
    val scored = lineup.score_info.end.scored - lineup.score_info.start.scored
    val allowed = lineup.score_info.end.allowed - lineup.score_info.start.allowed
    val last_game_event =  prev_lineup.flatMap(_.raw_game_events.lastOption)
    val (team_possessions, opp_possessions, proc_events, adjust_prev_lineup) =
      calculate_possessions(lineup.raw_game_events, last_game_event)

    val adjusted_prev_lineup = prev_lineup.map { prev => (last_game_event, adjust_prev_lineup) match {
      case (Some(last_ev), true) if last_ev.team_possession.nonEmpty =>
        prev.copy(
          team_stats = prev.team_stats.copy(
            num_possessions = prev.team_stats.num_possessions - 1
          )
        )
      case (Some(last_ev), true) if last_ev.opponent_possession.nonEmpty =>
        prev.copy(
          opponent_stats = prev.opponent_stats.copy(
            num_possessions = prev.opponent_stats.num_possessions - 1
          )
        )
      case _ => prev
    }}

    lineup.copy(
      team_stats = lineup.team_stats.copy(
        num_events = lineup.raw_game_events.filter(_.team.isDefined).size,
        num_possessions = team_possessions
//TODO
//(haven't decided if I want running possession counts yet, if I did it would look like:)
//        + adjusted_prev_lineup.map(_.team_stats.num_possessions).getOrElse(0)
        ,
        pts = scored,
        plus_minus = scored - allowed
      ),
      opponent_stats = lineup.opponent_stats.copy(
        num_events = lineup.raw_game_events.filter(_.opponent.isDefined).size, //TODO exclude subs
        num_possessions = opp_possessions
//TODO
//(haven't decided if I want running possession counts yet, if I did it would look like:)
//+ adjusted_prev_lineup.map(_.opponent_stats.num_possessions).getOrElse(0)
        ,
        pts = allowed,
        plus_minus = allowed - scored
      ),
      raw_game_events = proc_events
    ) -> adjusted_prev_lineup
  }

  /** There is a weird bug that has happened one time where the scores got swapped */
  def fix_possible_score_swap_bug(lineup: List[LineupEvent], box_lineup: LineupEvent)
    : List[LineupEvent] =
  {
    def scores_reversed(g1: Game.Score, g2: Game.Score): Boolean = {
      (g1.scored == g2.allowed) && (g2.scored == g1.allowed)
    }
    lineup.lastOption.map(_.score_info) match {
      case Some(LineupEvent.ScoreInfo(_, final_score, _, _))
        if scores_reversed(final_score, box_lineup.score_info.end)
      =>
        def reverse_score(score: Game.Score): Game.Score =
          score.copy(allowed = score.scored, scored = score.allowed)
        def reverse_scores(score_info: LineupEvent.ScoreInfo): LineupEvent.ScoreInfo = {
          score_info.copy(
            start = reverse_score(score_info.start),
            end = reverse_score(score_info.end),
            start_diff = -score_info.start_diff,
            end_diff = -score_info.end_diff
          )
        }
        lineup.map { x =>
          val t_pts = x.team_stats.pts
          val o_pts = x.opponent_stats.pts
          x.copy(
            score_info = reverse_scores(x.score_info),
            team_stats = x.team_stats.copy(
              pts = o_pts,
              plus_minus = -x.team_stats.plus_minus
            ),
            opponent_stats = x.opponent_stats.copy(
              pts = t_pts,
              plus_minus = -x.opponent_stats.plus_minus
            )
          )
        }
      case _ => lineup //(we're good, nothing to do)
    }
  }

  /** Returns team and opponent possessions based on the raw event data */
  protected def calculate_possessions(
    raw_events: Seq[LineupEvent.RawGameEvent], last_event: Option[LineupEvent.RawGameEvent]
  ): (Int, Int, List[LineupEvent.RawGameEvent], Boolean) =
  {
    /** (only "opponent events" can be these by construction) */
    def is_substitution_event(event_str: String): Boolean = {
      (Some(event_str), None) match {
        case EventUtils.ParseTeamSubIn(_) => true
        case EventUtils.ParseTeamSubOut(_) => true
        case _ => false
      }
    }

    /** Handles all the events that never indicate a change in possession */
    def is_ignorable_game_event(event_str: String): Boolean = Some(event_str) match {

      // Different cases:
      // 1.1] Jump ball
      case EventUtils.ParseJumpballWonOrLost(_) => true

      // 1.2] Timeout
      case EventUtils.ParseTimeout(_) => true

      // 2.1] Blocks (wait for rebound to decide if the possession changes)
      // New:
      //RawGameEvent(Some("14:11:00,7-9,Darryl Morsell, 2pt layup blocked missed"), None),
      //RawGameEvent(None, Some("14:11:00,7-9,Emmitt Williams, block")),
      //RawGameEvent(Some("14:11:00,7-9,Team, rebound offensivedeadball"), None),
      // Legacy:
      //"team": "04:53,55-69,LAYMAN,JAKE Blocked Shot"
      //"opponent": "04:53,55-69,TEAM Offensive Rebound"
      case EventUtils.ParseShotBlocked(_) => true

      // 2.2] Steals - possession always changes but we'll wait for the offensive action
      case EventUtils.ParseStolen(_) => true

      // 3.1] Personal Fouls
      // New:
      //RawGameEvent(None, Some("13:36:00,7-9,Team, rebound offensivedeadball")),
      //RawGameEvent(Some("13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow"), None)
      // Legacy:
      //"opponent": "10:00,51-60,MYKHAILIUK,SVI Commits Foul"
      case EventUtils.ParsePersonalFoul(_) => true

      // 3.2] Technical fouls
      // We're going to treat the technical foul like an additional possession
      // because otherwise it's going to be complicated
      case EventUtils.ParseTechnicalFoul(_) => true

      // 3.3] Foul info
      // Contains no possession related value
      case EventUtils.ParseFoulInfo(_) => true

      case _ => false
    }

    /** For "opponent events" we ignore subs _or_ "ignorable game events" */
    def is_ignorable_event(event_str: String): Boolean =
      is_substitution_event(event_str) || is_ignorable_game_event(event_str)

    /** Which team is in possession */
    object Direction extends Enumeration {
      val Init, Team, Opponent = Value
    }
    /** State for building possession events */
    case class PossState(
      team: Int, opponent: Int,
      events: List[LineupEvent.RawGameEvent],
      direction: Direction.Value,
      adjust_prev_lineup: Boolean,
      concurrency: PossState.ConcurrencyState
    )
    object PossState {
      /** Starting state */
      def init: PossState  = PossState(
        0, 0, Nil, Direction.Init, false, PossState.ConcurrencyState.init
      )
      /** Handles concurrency issues with the input data */
      case class ConcurrencyState(
        last_date_str: String,
        start_possession_events: List[LineupEvent.RawGameEvent],
        end_possession_events: List[LineupEvent.RawGameEvent]
      )
      object ConcurrencyState {
        /** Starting state */
        def init: ConcurrencyState = ConcurrencyState("", Nil, Nil)
      }
    }

    /** Adds the possession count to the event */
    def enrich(state: PossState, ev: LineupEvent.RawGameEvent): PossState = {
      val enriched_event = ev.copy(
        team_possession = if (state.direction == Direction.Team) Some(state.team) else None,
        opponent_possession = if (state.direction == Direction.Opponent) Some(state.opponent) else None
      )
      state.copy(
        events = enriched_event :: state.events,
        concurrency = state.concurrency.copy(
          last_date_str = ev.get_date_str
        )
      )
    }

    /** First non-ignorable event ... are we "stealing" the last lineup's final possession */
    def check_for_prev_lineup_adjustment(
      ev: LineupEvent.RawGameEvent, prev_event: Option[LineupEvent.RawGameEvent]
    ): Boolean = (ev, prev_event) match {
      case (LineupEvent.RawGameEvent.Team(_), Some(prev)) if prev.team_possession.nonEmpty =>
        true
      case (LineupEvent.RawGameEvent.Opponent(_), Some(prev)) if prev.opponent_possession.nonEmpty =>
        true
      case _ => false
    }

    /** The events often come in the wrong order if they have the same timestamp,
        so we'll catch that case and then re-order them into something saner
        (basically - if an event will cause a possession change then it goes at the end)
    */
    def concurrent_event_handler
      : PartialFunction[(PossState, LineupEvent.RawGameEvent), PossState] =
    {
      // First time we see an event with the same timestamp as the previous one
      case (state, ev)
        if (ev.get_date_str == state.concurrency.last_date_str) &&
            state.concurrency.start_possession_events.isEmpty
      =>
        // Pull the last element out and re-run
        concurrent_event_handler(state.copy(
          events = state.events.tail, // exists by construction
          concurrency = state.concurrency.copy(
            start_possession_events = state.events.head :: Nil // exists by construction
          )
        ), ev) //(can't recurse >1 by construction)

      // Once we're in a concurrency event:

      case (state, ev) if (ev.get_date_str == state.concurrency.last_date_str) => (ev match {

        // Obvious cases: if this event is in the "expected" direction

        case ev @ LineupEvent.RawGameEvent.Opponent(opp_info)
          if (state.direction == Direction.Opponent)
        =>
          state.concurrency.copy(
            start_possession_events = ev :: state.concurrency.start_possession_events
          )

        case ev @ LineupEvent.RawGameEvent.Team(opp_info)
          if (state.direction == Direction.Team)
        =>
          state.concurrency.copy(
            start_possession_events = ev :: state.concurrency.start_possession_events
          )

        // If the event is in the "unexpected" direction but is ignorable then don't reorder

        case ev @ LineupEvent.RawGameEvent.Opponent(opp_info)
          if (state.direction == Direction.Team) && is_ignorable_event(opp_info)
        =>
          state.concurrency.copy(
            start_possession_events = ev :: state.concurrency.start_possession_events
          )

        case ev @ LineupEvent.RawGameEvent.Team(team_info)
          if (state.direction == Direction.Opponent) && is_ignorable_game_event(team_info)
        =>
          state.concurrency.copy(
            start_possession_events = ev :: state.concurrency.start_possession_events
          )

        // Otherwise we're going to put it in the opposite direction

        case _ =>
          state.concurrency.copy(
            end_possession_events = ev :: state.concurrency.end_possession_events
          )
      }) match {
        case new_concurrency =>
          state.copy(concurrency = new_concurrency)
      }
    }

    /** We have a complete set of events with the same timestamp, re-order them */
    def complete_concurrency_event(state: PossState): List[LineupEvent.RawGameEvent] = {
      state.concurrency.start_possession_events.reverse ++ state.concurrency.end_possession_events.reverse
    }

    /** Handles state transitions to the possession list as events come in */
    def normal_state_transition: (PossState, LineupEvent.RawGameEvent) => PossState =
    {
      case (state, ev @ LineupEvent.RawGameEvent.Opponent(opp_info)) if is_ignorable_event(opp_info) =>
        enrich(state, ev) //(ignore sub data or selected game data - see above)
      case (state, ev @ LineupEvent.RawGameEvent.Team(team_info)) if is_ignorable_game_event(team_info) =>
        enrich(state, ev) //(ignore selected game data - see above)

      case (state @ PossState(_, _, _, Direction.Init, _, _), ev @ LineupEvent.RawGameEvent.Opponent(_)) =>
        enrich(state.copy(
          opponent = 1, direction = Direction.Opponent,
          adjust_prev_lineup = check_for_prev_lineup_adjustment(ev, last_event)
        ), ev)
      case (state @ PossState(_, _, _, Direction.Init, _, _), ev @ LineupEvent.RawGameEvent.Team(_)) =>
        enrich(state.copy(
          team = 1, direction = Direction.Team,
          adjust_prev_lineup = check_for_prev_lineup_adjustment(ev, last_event)
        ), ev)

      case (state @ PossState(_, opp_poss, _, Direction.Team, _, _), ev @ LineupEvent.RawGameEvent.Opponent(_)) =>
        enrich(state.copy(opponent = opp_poss + 1, direction = Direction.Opponent), ev)
      case (state @ PossState(team_poss, _, _, Direction.Opponent, _, _), ev @ LineupEvent.RawGameEvent.Team(_)) =>
        enrich(state.copy(team = team_poss + 1, direction = Direction.Team), ev)
      case (state, ev) =>
        enrich(state, ev) //(all other cases just do nothing)
    }

    /** Once all the events are complete, "flush" the remaining concurrent events */
    def complete_lineup_events: PossState => PossState = {
      case state @ PossState(team, opp, events, _, adjust_prev_lineup, concurrency)
        if concurrency.start_possession_events.nonEmpty
      =>
        complete_concurrency_event(state).foldLeft(state)(normal_state_transition)

      case state => state
    }

    ((raw_events.foldLeft(PossState.init) { (state, ev) =>
      if (concurrent_event_handler.isDefinedAt(state, ev)) {
        // event has the same timestamp as the previous one, so we may need to do some re-ordering
        concurrent_event_handler(state, ev)
      } else {
        normal_state_transition(
          // Process any concurrent events that precede this one
          complete_concurrency_event(state).foldLeft(state)(normal_state_transition), ev
        )
      }
    }) match {
      case state =>
        complete_lineup_events(state)
    }) match {
      case PossState(team, opp, events, _, adjust_prev_lineup, _) =>
        (team, opp, events.reverse, adjust_prev_lineup)
    }
  }
}
object LineupUtils extends LineupUtils
