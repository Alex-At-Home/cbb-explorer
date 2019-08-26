package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

/** Utilities related to building up the lineup stats */
trait LineupUtils {
  import StateUtils.StateTypes._
  import ExtractorUtils._

  /** Useful scriptlet for checking results
  // Show results
  l.groupBy(t => (t.opponent, t.location_type)).mapValues(
    _.foldLeft((0,0))
    { (acc, v) => (acc._1 + v.team_stats.num_possessions,acc._2 + v.opponent_stats.num_possessions) }
  )
  // Compare to previous results
  (x1 zip x2).map { case (k, v) => k._1 -> (k._2._1 - v._2._1, k._2._2 - v._2._2) }
  */

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
        num_possessions = team_possessions,
        pts = scored,
        plus_minus = scored - allowed
      ),
      opponent_stats = lineup.opponent_stats.copy(
        num_events = lineup.raw_game_events.filter(_.opponent.isDefined).size, //TODO exclude subs
        num_possessions = opp_possessions,
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
      direction: Direction.Value,
      adjust_prev_lineup: Boolean
    )
    object PossState {
      /** Starting state */
      def init: PossState  = PossState(
        0, 0, Direction.Init, false
      )
      /** Handles concurrency issues with the input data */
      case class ConcurrencyState(
        last_date_str: String
      )
      object ConcurrencyState {
        /** Starting state */
        def init: ConcurrencyState = ConcurrencyState("")
      }
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

    /** Identify concurrent events (easy) */
    def check_for_concurrent_event(
      ev: Clumper.Event[PossState, PossState.ConcurrencyState, LineupEvent.RawGameEvent]
    ): (PossState.ConcurrencyState, Boolean) = ev match {
      case Clumper.Event(_, cs, Nil, ev) => //(first event always gens a clump, possibly of size 1)
        (cs.copy(last_date_str = ev.get_date_str), true)
      case Clumper.Event(_, cs, _, ev) if ev.get_date_str == cs.last_date_str =>
        (cs, true)
      case Clumper.Event(_, cs, _, ev) =>
        (cs.copy(last_date_str = ev.get_date_str), false)
    }

    /** Rearrange concurrent events to be a bit saner:
     *  (basically - if an event will cause a possession change then it goes at the end)
     */
    def rearrange_concurrent_event(
      s: PossState, cs: PossState.ConcurrencyState, reverse_clump: List[LineupEvent.RawGameEvent]
    ): List[LineupEvent.RawGameEvent] = {
      if (s.direction == Direction.Init) {
        reverse_clump.reverse
      } else {
        case class RearrangeState(
          start_possession_events: List[LineupEvent.RawGameEvent],
          end_possession_events: List[LineupEvent.RawGameEvent]
        ) {
          def direction(
            actual_dir: Direction.Value, ev: LineupEvent.RawGameEvent
          ): RearrangeState = {
            if (s.direction == actual_dir) {
              copy(start_possession_events = ev :: start_possession_events)
            } else {
              copy(end_possession_events = ev :: end_possession_events)
            }
          }
        }
        (reverse_clump.foldRight(RearrangeState(Nil, Nil)) {

          // Always put "ignorable" events in the order they were received
          case (ev, state) if is_ignorable_game_event(ev.info.getOrElse("")) =>
            state.direction(s.direction, ev)

          //  Normal processing:
          case (ev @ LineupEvent.RawGameEvent.Opponent(_), state) =>
            state.direction(Direction.Opponent, ev)
          case (ev @ LineupEvent.RawGameEvent.Team(_), state) =>
            state.direction(Direction.Team, ev)

        }) match {
          case RearrangeState(start_possession_events, end_possession_events) =>
            start_possession_events.reverse ++ end_possession_events.reverse
        }
      }
    }

    /** Manages splitting stream into concurrent chunks and then re-arranging them */
    val concurrent_event_handler = Clumper(
      PossState.ConcurrencyState.init,
      check_for_concurrent_event _,
      rearrange_concurrent_event _
    )

    /** Adds the possession count to the event */
    def enrich(state: PossState, ev: LineupEvent.RawGameEvent): (PossState, LineupEvent.RawGameEvent) = {
      val enriched_event = ev.copy(
        team_possession = if (state.direction == Direction.Team) Some(state.team) else None,
        opponent_possession = if (state.direction == Direction.Opponent) Some(state.opponent) else None
      )
      (state, enriched_event)
    }

    /** Handles state transitions to the possession list as events come in */
    def normal_state_transition: (PossState, LineupEvent.RawGameEvent) => (PossState, LineupEvent.RawGameEvent) =
    {
      case (state, ev @ LineupEvent.RawGameEvent.Opponent(opp_info)) if is_ignorable_event(opp_info) =>
        enrich(state, ev) //(ignore sub data or selected game data - see above)
      case (state, ev @ LineupEvent.RawGameEvent.Team(team_info)) if is_ignorable_game_event(team_info) =>
        enrich(state, ev) //(ignore selected game data - see above)

      case (state @ PossState(_, _, Direction.Init, _), ev @ LineupEvent.RawGameEvent.Opponent(_)) =>
        enrich(state.copy(
          opponent = 1, direction = Direction.Opponent,
          adjust_prev_lineup = check_for_prev_lineup_adjustment(ev, last_event)
        ), ev)
      case (state @ PossState(_, _, Direction.Init, _), ev @ LineupEvent.RawGameEvent.Team(_)) =>
        enrich(state.copy(
          team = 1, direction = Direction.Team,
          adjust_prev_lineup = check_for_prev_lineup_adjustment(ev, last_event)
        ), ev)

      case (state @ PossState(_, opp_poss, Direction.Team, _), ev @ LineupEvent.RawGameEvent.Opponent(_)) =>
        enrich(state.copy(opponent = opp_poss + 1, direction = Direction.Opponent), ev)
      case (state @ PossState(team_poss, _, Direction.Opponent, _), ev @ LineupEvent.RawGameEvent.Team(_)) =>
        enrich(state.copy(team = team_poss + 1, direction = Direction.Team), ev)
      case (state, ev) =>
        enrich(state, ev) //(all other cases just do nothing)
    }

    (StateUtils.foldLeft(raw_events, PossState.init, concurrent_event_handler) {
      case StateEvent.Next(ctx, state, event) => // Standard processing
        val (new_state, enriched_event) = normal_state_transition(state, event)
        ctx.stateChange(new_state, enriched_event)

      case StateEvent.Complete(ctx, _) => //(no additional processing when element list complete)
        ctx.noChange
    }) match {
      case FoldStateComplete(
        PossState(team, opp, _, adjust_prev_lineup),
        events
      ) =>
        (team, opp, events, adjust_prev_lineup)
    }
  }
}
object LineupUtils extends LineupUtils
