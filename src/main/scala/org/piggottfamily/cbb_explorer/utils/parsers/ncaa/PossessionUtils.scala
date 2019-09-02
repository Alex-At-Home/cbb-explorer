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
      val Normal, Error = Value
    }
  }

  protected case class ConcurrentClump(evs: List[Model.PlayByPlayEvent])

  protected sealed trait PossessionStatus
  protected case object PossessionArrowSwitch extends PossessionStatus
  protected case object PossessionError extends PossessionStatus
  protected case object PossessionContinues extends PossessionStatus
  /** compound_clump means that might have O and D possessions in the same clump */
  protected case class PossessionEnd(compound_clump: Boolean = false) extends PossessionStatus

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

  /** We generate this if the possession calcuations go wrong */
  val ERROR_EVENT = Model.OtherTeamEvent(0.0, Game.Score(0, 0), 0, "00:00,0-0,POSSESSION_STATE_ERROR")

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
    in_evs: List[Model.PlayByPlayEvent]
  ): PossessionStatus =
  {
    val defendingTeam = PossessionEvent(state.direction).DefendingTeam
    val attackingTeam = PossessionEvent(state.direction).AttackingTeam

    /** Ignore any events that have already been processed */
    def previously_processed: Model.PlayByPlayEvent => Boolean = {
      case ev: Model.MiscGameEvent if ev.poss > 0 => true
      case _ => false
    }

    /** Error out if the possession information is inconsistent with events */
    def invalid_possession_state(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseCommonDefensiveEvent(_)) => ()
      case defendingTeam(EventUtils.ParseCommonOffensiveEvent(_)) => ()
    }.nonEmpty

    // A note on rebounding:
    // Rebounds can be concurrent with missed shots ... for DRBs this is handled by the
    // high prio of that case (but explains the logic below for assigning different team/offensive
    // possesion numbers to the different events in the concurrentl clump), for ORBs this needs to
    // be specifically handled
    /** Example:
    13:19:00	Ryan Cline, rebound defensive	8-7
    13:19:00	Ryan Cline, block	8-7
    13:19:00		8-7	Darryl Morsell, 2pt drivinglayup blocked missed
    * And note you can have any of old-attacker/DRB/new-attacker in a single clump, eg
    * (OA) shot, (OD) foul, (OA) missed, (OD) drb, (OA) foul, (OD) fts, etc -
    * seems pretty unlikely you'd ever get more than 2 though, so can proceed on that basis
    * (but note the code that ensures you only look at unenriched events, which prevents
    *  an infinite recursion in that case ... we _should_ and do error out though)
    */

    /** This is the highest prio .. if the defending team rebounds it, they now have possession
     * (note fouls on the defense following a missed shot/rebound do generate DRB events)
    */
    def defensive_rebound(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case defendingTeam(EventUtils.ParseRebound(_)) => ()
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

    def technical_foul_defense(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case defendingTeam(EventUtils.ParseTechnicalFoul(_)) => ()
    }.nonEmpty

    def end_of_period(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case _: Model.MiscGameBreak => ()
    }.nonEmpty

    /** There's a bunch of different cases here:
     * If the missed free throw is not the final one, then you will always see a "deadball rebound", eg:
     (New) 04:28:00		52-59	Bruno Fernando, freethrow 1of2 missed [..]
           04:28:00		52-59	Team, rebound offensivedeadball
     (Old) 04:33,46-45,DAVISON,BRAD missed Free Throw
           04:33,46-45,TEAM Deadball Rebound
           04:33,47-45,DAVISON,BRAD made Free Throw

     * If the missed free throw is the final one, you may or may not see a rebound (ie it might) be in
     * the next clump (but then it's just like any other missed shot, ie "wait for the rebound")
     * Unfortunately figuring out last vs intermediate free throws is only possible (in old format)
     * by checking the scores
     */
    def handle_missed_freethrows(evs: List[Model.PlayByPlayEvent]): PossessionStatus = {
      evs.collect {
        case ev @ attackingTeam(EventUtils.ParseFreeThrowMade(_)) => ev
        case ev @ attackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ev
      }.collect {
        case ev: Model.MiscGameEvent => ev //(exposes .score, needed below)
      }.sortBy(ev => Game.Score.unapply(ev.score)).reverse match { // reverse => highest first
        case attackingTeam(EventUtils.ParseFreeThrowMade(_)) :: _ => //last free throw was made
          PossessionEnd()
        case attackingTeam(EventUtils.ParseFreeThrowMissed(_)) :: _ => //last free throw was missed
          PossessionContinues
      }
    }

    in_evs.filterNot(previously_processed) match {
      case evs if end_of_period(evs) => PossessionArrowSwitch

      case evs if defensive_rebound(evs) => PossessionEnd(compound_clump = true)
        // (note: defensive rebounds can be concurrent with misses)

      case evs if invalid_possession_state(evs) => PossessionError
        //(it's a bit hard to parse out clumps with DRBs so we'll ignore them for invalid state checks)

      case evs if offensive_turnover(evs) => PossessionEnd()

      case evs if technical_foul_defense(evs) => PossessionContinues
        //(if the defending team is called for a technical, the offensive team will get the ball back,
        // offensive technical fouls that result in a change of possession also generate an
        // offensive turnover event, so can ignore)

      case evs if made_shot(evs) && missed_and_one(evs) => PossessionContinues //(wait for the rebound)
        //(any free throws must be an and-1 because technical fouls are handled above)
      case evs if made_shot(evs) => PossessionEnd() //(no and-1 because of order)

      case evs if fts_some_missed(evs) => handle_missed_freethrows(evs) //untangle what happened
      case evs if fts_some_made(evs) => PossessionEnd() //(none missed because of order)

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
      //(never overwrite a previous possession, ie ev_poss > 0)
      case game_ev: Model.MiscGameEvent if (state.direction == Direction.Team) && game_ev.poss <= 0 =>
        game_ev.with_poss(state.team)
      case game_ev: Model.MiscGameEvent if (state.direction == Direction.Opponent) && game_ev.poss <= 0 =>
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

      case PossessionError => //latch the error state on, will stop trying to calc possessions
        val new_state = state.copy(status = PossState.Status.Error)
        val dummy_event = ERROR_EVENT
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
        val attackingTeam = PossessionEvent(state.direction).AttackingTeam
        val new_state = switch_state(state)
        val partially_enriched_evs = evs.map {
          case ev @ attackingTeam(_) => enrich(state, ev) //(use old state)
          case ev => ev //(will get enriched next on the recursion below)
        }
        handle_clump(new_state, partially_enriched_evs)

      case PossessionEnd(false) =>
        val attackingTeam = PossessionEvent(state.direction).AttackingTeam
        val new_state = switch_state(state)
        (new_state, evs.map(ev => enrich(state, ev)))

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
