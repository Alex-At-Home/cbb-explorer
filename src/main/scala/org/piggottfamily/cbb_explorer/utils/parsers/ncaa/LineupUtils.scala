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

/** TODO`
val (team_possessions, opp_possessions, proc_events, adjust_prev_lineup) =
  calculate_possessions(lineup.raw_game_events, last_game_event)
*/

  /** TODO build the stats from the game events */
  def enrich_lineup(lineup: LineupEvent): LineupEvent =
  {
    val scored = lineup.score_info.end.scored - lineup.score_info.start.scored
    val allowed = lineup.score_info.end.allowed - lineup.score_info.start.allowed
    val last_game_event =  prev_lineup.flatMap(_.raw_game_events.lastOption)

    lineup.copy(
      team_stats = lineup.team_stats.copy(
        num_events = lineup.raw_game_events.filter(_.team.isDefined).size,
        num_possessions = 0, //TODO: enrich based on raw events
        pts = scored,
        plus_minus = scored - allowed
      ),
      opponent_stats = lineup.opponent_stats.copy(
        num_events = lineup.raw_game_events.filter(_.opponent.isDefined).size, //TODO exclude subs
        num_possessions = 0, //TODO as above
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
  def calculate_possessions(
    raw_events: Seq[LineupEvent.RawGameEvent]
  ): List[LineupEvent.RawGameEvent] =
  {
    // Lots of data modelling:

    /** Which team is in possession */
    object Direction extends Enumeration {
      val Init, Team, Opponent = Value

      /** Switch direction */
      def opposite(dir: Direction.Value): Direction.Value = dir match {
        case Direction.Init => dir
        case Direction.Team = Direction.Opponent
        case Direction.Opponent = Direction.Team
      }
    }
    /** State for building possession events */
    case class PossState(
      team: Int, opponent: Int,
      direction: Direction.Value,
      possession_unclear: Boolean
    )
    object PossState {
      /** Starting state */
      def init: PossState  = PossState(
        0, 0, Direction.Init, false
      )
    }

    case class ConcurrentClump(evs: List[LineupEvent.RawGameEvent])

    sealed trait PossessionStatus
    case object PossessionUnclear extends PossessionStatus
    case object PossessionContinues extends PossessionStatus
    case class PossessionEnd(last_clump: Boolean = false) extends PossessionStatus

    case class PossessionEvent(dir: Direction.Value) {
      /** The team in possession */
      object AttackingTeam {
        def unapply(x: RawGameEvent): Option[String] = dir match {
          case Direction.Team => x.team
          case Direction.Opponent => x.opponent
          case _ => None
        }
      }
      /** The team not in possession */
      object DefendingTeam {
        def unapply(x: RawGameEvent): Option[String] = dir match {
          case Direction.Team => x.opponent
          case Direction.Opponent => x.opponent
          case _ => None
        }
      }
    }

    /** Adds possession count to raw event */
    def enrich(state: PossState, ev: LineupEvent.RawGameEvent): LineupEvent.RawGameEvent = {
      val enriched_event = ev.copy(
        team_possession = if (state.direction == Direction.Team) Some(state.team) else None,
        opponent_possession = if (state.direction == Direction.Opponent) Some(state.opponent) else None
      )
      enriched_event
    }

    /** Figure out who has possession to start the game */
    def first_possession_status(evs: List[LineupEvent.RawGameEvent]): Option[Direction.Value] = {
      evs.collect {
        case RawGameEvent.Team(EventUtils.ParseCommonOffensiveEvent(_)) =>
          Some(Direction.Team)
        case RawGameEvent.Team(EventUtils.ParseCommonDefensiveEvent(_)) =>
          Some(Direction.Opponent)
        case RawGameEvent.Opponent(EventUtils.ParseCommonOffensiveEvent(_)) =>
          Some(Direction.Opponent)
        case RawGameEvent.Opponent(EventUtils.ParseCommonDefensiveEvent(_)) =>
          Some(Direction.Team)
      }.headOption
    }

    /** Process the events within a concurrent clump to see if the possession is changing/has changed */
    def clump_possession_status(
      state: PossState,
      evs: List[LineupEvent.RawGameEvent]
    ): PossessionStatus =
    {
      val dir = state.direction
      /** This is the highest prio .. if the defending team rebounds it, they now have possession */
      def defensive_rebound(evs: List[LineupEvent.RawGameEvent]): Boolean = {
        evs.collect {
          case PossessionEvent(dir).DefendingTeam(EventUtils.Rebound(_)) =>
        }.nonEmpty
      }

      def offensive_rebound(evs: List[LineupEvent.RawGameEvent]): Boolean = {
        evs.collect {
          case PossessionEvent(dir).AttackingTeam(EventUtils.Rebound(_)) =>
        }.nonEmpty
      }

      /** Offensive turnover */
      def offensive_turnover(evs: List[LineupEvent.RawGameEvent]): Boolean = {
        evs.collect {
          case PossessionEvent(dir).AttackingTeam(EventUtils.Turnover(_)) =>
        }.nonEmpty
      }

      /** Made shot and missed the and one ... need to wait for DRB */
      def made_shot_and_missed_and_one(evs: List[LineupEvent.RawGameEvent]): Boolean = {
        made_shot(evs) &&
        ev.collect {
          case PossessionEvent(dir).AttackingTeam(EventUtils.ParseFreeThrowMissed(_)) =>
        }.nonEmpty
      }

      /** Made shot (use order of clauses to ensure there was no missed and-1) */
      def made_shot(evs: List[LineupEvent.RawGameEvent]): Boolean = {
        evs.collect {
          case PossessionEvent(dir).AttackingTeam(EventUtils.ParseShotMade(_)) =>
        }.nonEmpty
      }

      /** Took free throws and hit at least one of them */
      def fts_some_made(evs: List[LineupEvent.RawGameEvent]): Boolean = {
        evs.collect {
          case PossessionEvent(dir).ParseFreeThrowMade(EventUtils.ParseShotMade(_)) =>
        }.nonEmpty
      }

      /** Took free throws and missed at least one of them */
      def fts_some_missed(evs: List[LineupEvent.RawGameEvent]): Boolean = {
        evs.collect {
          case PossessionEvent(dir).ParseFreeThrowMade(EventUtils.ParseShotMissed(_)) =>
        }.nonEmpty
      }

      //TODO: end of period

      //TODO: error on inconsistencies rather than silently give the wrong possession count

      evs match {
        case _ if defensive_rebound(evs) => PossessionEnd(last_clump = true)
        case _ if state.possession_unclear && !offensive_rebound(evs) => PossessionEnd(last_clump = true)

        case _ if offensive_turnover(evs) => PossessionEnd()
        case _ if made_shot_and_missed_and_one(evs) => PossessionContinues //(wait for the rebound)
        case _ if made_shot(evs) => PossessionEnd()

        case _ if fts_some_missed(evs) && fts_some_made(evs) => PossessionUnclear
          //(the problem here is that with legacy data we don't know if the last one missed ...
          // what happens is if there's no rebound on either side then possession changes
          // if there's a DRB possession changes are per usual, and otherwise (==ORB) possession continues)

        case _ if fts_some_missed(evs) => PossessionContinues //(all FTs missed => last FT missed => wait for the rebound)
        case _ if fts_some_made(evs) => PossessionEnd()
        case _ => PossessionContinues
      }
    }

    /** Sort out state if possession changes */
    def switch_state(state: PossState): PossState = {
      if (state.direction == Direction.Team) {
        state.copy(
          opponent = state.opponent + 1, direction = Direction.Opponent,
          possession_unclear = false
        )
      } else { //(must be Opponent, not Init, by construction)
        state.copy(
          team = state.team + 1, direction = Direction.Team,
          possession_unclear = false
        )
      }
    }

    /** Updates the state and enriches the events with poss number based on incoming clump */
    def handle_clump(
      state: PossState, evs: List[LineupEvent.RawGameEvent]
    ): (PossState, List[LineupEvent.RawGameEvent]) = clump_possession_status(state, evs) match {
      case PossessionEnd(true) =>
        val new_state = switch_state(state)
        handle_clump(new_state, evs) //(can't recurse more than once by construction)
      case PossessionEnd(false) =>
        val new_state = switch_state(state)
        (new_state, evs.map(ev => enrich(new_state, ev)))
      case PossessionUnclear =>
        (state.copy(possession_unclear = true), evs.map(ev => enrich(state, ev)))
      case PossessionContinues =>
        (state.copy(possession_unclear = false), evs.map(ev => enrich(state, ev)))
    }

    //TODO: clumper
    val clumped_events = raw_events.map(ev => ConcurrentClump(List(ev)))
    (StateUtils.foldLeft(clumped_events, PossState.init, classOf[LineupEvent.RawGameEvent]) {

      case StateEvent.Next(ctx, state, ConcurrentClump(evs)) if state.direction == Direction.Init =>
        // First state, who has the first possession?
        first_possession_status(evs) match {
          case Some(dir) =>
            val new_state = switch_state(state.copy(direction = Direction.opposite(dir)))
            handle_clump(new_state, evs)
          case _ =>
            (state, ev) //(do nothing, wait for next clump)
        }

      case StateEvent.Next(ctx, state, ConcurrentClump(evs)) => // Standard processing
        val (new_state, enriched_evs) = handle_clump(state, evs)
        ctx.stateChange(new_state, enriched_evs)

      case StateEvent.Complete(ctx, _) => //(no additional processing when element list complete)
        ctx.noChange

    }) match {
      case FoldStateComplete(_, events) =>
        events
    }

  } //(end calculate_possessions)
}
object LineupUtils extends LineupUtils
