package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

/** Utilities related to calculation possession from raw game events */
trait PossessionUtils {
  import ExtractorUtils._
  import StateUtils.StateTypes._

  // Lots of data modelling:

  /** Which team is in possession */
  protected object Direction extends Enumeration {
    val Init, Team, Opponent = Value

    /** Switch direction */
    def opposite(dir: Direction.Value): Direction.Value = dir match {
      case Direction.Init => dir
      case Direction.Team => Direction.Opponent
      case Direction.Opponent => Direction.Team
    }
  }
  /** State for building possession events */
  protected case class PossState(
    team: Int, opponent: Int,
    direction: Direction.Value,
    possession_arrow: Direction.Value, //(who gets possession next game break)
    status: PossState.Status.Value
  ) {
    def unclear: Boolean = status == PossState.Status.Unclear
    def broken: Boolean = status == PossState.Status.Error
  }
  protected object PossState {
    /** Starting state */
    def init: PossState  = PossState(
      0, 0, Direction.Init, Direction.Init, Status.Normal
    )
    /** Handles concurrency issues with the input data */
    case class ConcurrencyState(
      last_min: Double
    )
    object ConcurrencyState {
      /** Starting state */
      def init: ConcurrencyState = ConcurrencyState(-1.0)
    }
    object Status extends Enumeration {
      val Normal, Unclear, Error = Value
    }
  }

  protected case class ConcurrentClump(evs: List[Model.PlayByPlayEvent])

  protected sealed trait PossessionStatus
  protected case object PossessionArrowSwitch extends PossessionStatus
  protected case object PossessionUnclear extends PossessionStatus
  protected case object PossessionError extends PossessionStatus
  protected case object PossessionContinues extends PossessionStatus
  protected case class PossessionEnd(last_clump: Boolean = false) extends PossessionStatus

  protected case class PossessionEvent(dir: Direction.Value) {
    /** The team in possession */
    object AttackingTeam {
      def unapply(x: Model.PlayByPlayEvent): Option[String] = x match {
        case Model.OtherTeamEvent(_, _, _, event_str) if dir == Direction.Team => Some(event_str)
        case Model.OtherOpponentEvent(_, _, _, event_str) if dir == Direction.Opponent => Some(event_str)
        case _ => None
      }
    }
    /** The team not in possession */
    object DefendingTeam {
      def unapply(x: Model.PlayByPlayEvent): Option[String] = x match {
        case Model.OtherTeamEvent(_, _, _, event_str) if dir == Direction.Opponent => Some(event_str)
        case Model.OtherOpponentEvent(_, _, _, event_str) if dir == Direction.Team => Some(event_str)
        case _ => None
      }
    }
  }

  /** Util methods */

  /** Identify concurrent events (easy) */
  protected def check_for_concurrent_event(
    ev: Clumper.Event[PossState, PossState.ConcurrencyState, ConcurrentClump]
  ): (PossState.ConcurrencyState, Boolean) = ev match {
    case Clumper.Event(_, cs, Nil, ConcurrentClump(ev :: _)) =>
      //(first event always gens a clump, possibly of size 1)
      (cs.copy(last_min = ev.min), true)
    case Clumper.Event(_, cs, _, ConcurrentClump((ev: Model.MiscGameBreak) :: _)) =>
      // Game breaks can never be part of a clump
      (cs.copy(last_min = -1.0), false)
    case Clumper.Event(_, cs, _, ConcurrentClump(ev :: _)) if ev.min == cs.last_min =>
      (cs, true)
    case Clumper.Event(_, cs, _, ConcurrentClump(ev :: _)) =>
      (cs.copy(last_min = ev.min), false)
    case Clumper.Event(_, cs, _, ConcurrentClump(Nil)) => //(empty ConcurrentClump, not possible by construction)
      (cs, true)
  }

  /** Aggregates all concurrent clumps of 1 event into a single clump of many events */
  protected def rearrange_concurrent_event(
    s: PossState, cs: PossState.ConcurrencyState, reverse_clump: List[ConcurrentClump]
  ): List[ConcurrentClump] = {
    List(ConcurrentClump(
      reverse_clump.foldLeft(List[Model.PlayByPlayEvent]()) { (acc, v) => v.evs ++ acc } //(re-reverses it)
    ))
  }

  /** Manages splitting stream into concurrent chunks and then combining them */
  protected val concurrent_event_handler = Clumper(
    PossState.ConcurrencyState.init,
    check_for_concurrent_event _,
    rearrange_concurrent_event _
  )

  /** Figure out who has possession to start the game */
  protected def first_possession_status(evs: List[Model.PlayByPlayEvent]): Option[Direction.Value] = {
    evs.collect {
      case Model.OtherTeamEvent(_, _, _, EventUtils.ParseCommonOffensiveEvent(_)) =>
        Direction.Team
      case Model.OtherTeamEvent(_, _, _, EventUtils.ParseCommonDefensiveEvent(_)) =>
        Direction.Opponent
      case Model.OtherOpponentEvent(_, _, _, EventUtils.ParseCommonOffensiveEvent(_)) =>
        Direction.Opponent
      case Model.OtherOpponentEvent(_, _, _, EventUtils.ParseCommonDefensiveEvent(_)) =>
        Direction.Team
    }.headOption
  }

  /** Process the events within a concurrent clump to see if the possession is changing/has changed */
  protected def clump_possession_status(
    state: PossState,
    evs: List[Model.PlayByPlayEvent]
  ): PossessionStatus =
  {
    val defendingTeam = PossessionEvent(state.direction).DefendingTeam
    val attackingTeam = PossessionEvent(state.direction).AttackingTeam

    /** Error out if the possession information is inconsistent with events */
    def invalid_possession_state(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseCommonDefensiveEvent(_)) => ()
      case defendingTeam(EventUtils.ParseCommonOffensiveEvent(_)) => ()
    }.nonEmpty

    /** This is the highest prio .. if the defending team rebounds it, they now have possession */
    def defensive_rebound(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case defendingTeam(EventUtils.ParseRebound(_)) => ()
    }.nonEmpty

    def offensive_rebound(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseRebound(_)) => ()
    }.nonEmpty

    /** Offensive turnover */
    def offensive_turnover(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseTurnover(_)) => ()
    }.nonEmpty

    /** Made shot and missed the and one ... need to wait for DRB */
    def missed_and_one(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ()
    }.nonEmpty

    /** Made shot (use order of clauses to ensure there was no missed and-1) */
    def made_shot(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseShotMade(_)) => ()
    }.nonEmpty

    /** Took free throws and hit at least one of them */
    def fts_some_made(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseFreeThrowMade(_)) => ()
    }.nonEmpty

    /** Took free throws and missed at least one of them */
    def fts_some_missed(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ()
    }.nonEmpty

    def technical_foul(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case defendingTeam(EventUtils.ParseTechnicalFoul(_)) => ()
    }.nonEmpty

    def end_of_period(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case _: Model.MiscGameBreak => ()
    }.nonEmpty

    evs match {
      case _ if invalid_possession_state(evs) => PossessionError

      case _ if end_of_period(evs) => PossessionArrowSwitch

      case _ if defensive_rebound(evs) => PossessionEnd(last_clump = true)

      case _ if state.unclear && !offensive_rebound(evs) => PossessionEnd(last_clump = true)
        //(see the case that generates PossessionUnclear, below)

      case _ if offensive_turnover(evs) => PossessionEnd()

      case _ if technical_foul(evs) => PossessionContinues
        //(if the defending team is called for a technical, the offensive team will get the ball back)

      case _ if made_shot(evs) && missed_and_one(evs) => PossessionContinues //(wait for the rebound)
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

  /** Returns team and opponent possessions based on the raw event data */
  def calculate_possessions(
    raw_events: Seq[Model.PlayByPlayEvent]
  ): List[Model.PlayByPlayEvent] =
  {
    /** Adds possession count to raw event */
    def enrich(state: PossState, ev: Model.PlayByPlayEvent): Model.PlayByPlayEvent = ev match {
      case game_ev: Model.MiscGameEvent if (state.direction == Direction.Team) =>
        game_ev.with_poss(state.team)
      case game_ev: Model.MiscGameEvent if (state.direction == Direction.Opponent) =>
        game_ev.with_poss(state.opponent)
      case _ => ev
    }

    /** Sort out state if possession changes */
    def switch_state(state: PossState): PossState = {
      if (state.direction == Direction.Team) {
        state.copy(
          opponent = state.opponent + 1, direction = Direction.Opponent,
          status = PossState.Status.Normal
        )
      } else { //(must be Opponent, not Init, by construction)
        state.copy(
          team = state.team + 1, direction = Direction.Team,
          status = PossState.Status.Normal
        )
      }
    }

    /** Updates the state and enriches the events with poss number based on incoming clump */
    def handle_clump(
      state: PossState, evs: List[Model.PlayByPlayEvent]
    ): (PossState, List[Model.PlayByPlayEvent]) = clump_possession_status(state, evs) match {

      case PossessionError =>
        val new_state = state.copy(status = PossState.Status.Error)
        val dummy_event = Model.OtherTeamEvent(0.0, Game.Score(0, 0), 0, "00:00,0-0,POSSESSION_STATE_ERROR")
        (new_state, dummy_event :: evs)

      case PossessionArrowSwitch =>
        val new_state = switch_state(state).copy(
          direction = state.possession_arrow,
          possession_arrow = Direction.opposite(state.possession_arrow)
        )
        //Game break meaning whoever has the possession arrow gets the ball, so may or may not`
        //get consecutive possessions on either side
        (new_state, evs.map(ev => enrich(state, ev)))

      case PossessionEnd(true) =>
        val new_state = switch_state(state)
        handle_clump(new_state, evs) //(can't recurse more than once by construction)
      case PossessionEnd(false) =>
        val new_state = switch_state(state)
        (new_state, evs.map(ev => enrich(state, ev)))
        //(use the old stqte because it's the _next_ clump that belongs to the update possession count)
      case PossessionUnclear =>
        (state.copy(status = PossState.Status.Unclear), evs.map(ev => enrich(state, ev)))
      case PossessionContinues =>
        (state.copy(status = PossState.Status.Normal), evs.map(ev => enrich(state, ev)))
    }

    val clumped_events = raw_events.map(ev => ConcurrentClump(ev :: Nil))
    (StateUtils.foldLeft(
      clumped_events, PossState.init, classOf[Model.PlayByPlayEvent], concurrent_event_handler
    ) {
      case StateEvent.Next(ctx, state, ConcurrentClump(evs)) if state.broken =>
        // An error occurred so we're going to stop calculate possessions for this game
        ctx.stateChange(state, evs)

      case StateEvent.Next(ctx, state, ConcurrentClump(evs)) if state.direction == Direction.Init =>
        // First state, who has the first possession?
        val (new_state, enriched_evs) = first_possession_status(evs) match {
          case Some(dir) =>
            val opposite_dir = Direction.opposite(dir)
            val new_state = switch_state(state.copy( //(abuse switch_state for 1st poss)
              direction = opposite_dir, possession_arrow = opposite_dir
            ))
            handle_clump(new_state, evs)
          case _ =>
            (state, evs) //(do nothing, wait for next clump)
        }
        ctx.stateChange(new_state, enriched_evs)

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
object PossessionUtils extends PossessionUtils
