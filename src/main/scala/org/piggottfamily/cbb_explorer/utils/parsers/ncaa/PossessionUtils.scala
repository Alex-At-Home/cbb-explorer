package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import scala.util.Try

/** Utilities related to calculation possession from raw game events */
trait PossessionUtils {
  import ExtractorUtils._
  import StateUtils.StateTypes._

//TODO: handle jump ball, switches the possession arrow....

  /** Useful scriptlet for checking results
  // Show results (checl)
  l.groupBy(t => (t.opponent, t.location_type)).mapValues(
    _.foldLeft((0,0))
    { (acc, v) => (acc._1 + v.team_stats.num_possessions,acc._2 + v.opponent_stats.num_possessions) }
  )
  // Compare to previous results
  (x1 zip x2).map { case (k, v) => k._1 -> (k._2._1 - v._2._1, k._2._2 - v._2._2) }

  //Or (checking vs raw game events):
  l.groupBy(t => (t.opponent, t.location_type)).mapValues { ev =>
    ev.headOption.map(_.date) -> ev.flatMap(_.raw_game_events).reverse.flatMap(ev => ev.team_possession.orElse(ev.opponent_possession)).headOption
  }.toList.sortBy(_._2._1.toString)

  // Number of missing possessions:
  l.groupBy(t => (t.opponent, t.location_type)).mapValues { ev =>
     ev.headOption.map(_.date) -> ev.flatMap(_.raw_game_events).count(ev => ev.toString.contains("MISSING_POSSESSION"))
  }.toList.sortBy(_._2._1.toString)
  */

/** Problems:

NCAT: missed an and-one, so over-counted FTs
19:10:00	Tyrone Lyons, 2pt jumpshot 2ndchance;fromturnover;pointsinthepaint made	27-49
19:06:00		27-49	Bruno Fernando, foul personal shooting;1freethrow
19:06:00	Tyrone Lyons, foulon	27-49
19:06:00	Tyrone Lyons, freethrow 1of1 2ndchance;fastbreak;fromturnover made
(fixed this ... but .....)
Ah but if you miss the front-end of a 1-1 then you get it wrong also...
00:50:40	Ty Jerome, freethrow 1of1 fastbreak missed	70-65
00:50:40		70-65	Bruno Fernando, rebound defensive
00:50:40		70-65	Aaron Wiggins, foul personal oneandone

UVA discrepancy is weird:
[INFO] Parsed box score: opponent=[TeamSeasonId(TeamId(Virginia),Year(2018))] venue=[Home]
--------------------
[50 - 10 + 7 + 14 = 61]
[59 - 9 + ~6~ 7 + 2 = ~58~ 59] so still a diff of 2 even with the FT fix

*/

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
    team_stats: StatsFragment,
    opponent_stats: StatsFragment
  )
  protected object PossState {
    /** Starting state */
    def init: PossState  = PossState(
      0, 0, Direction.Init, Direction.Init, StatsFragment(), StatsFragment()
    )
    /** Handles concurrency issues with the input data */
    case class ConcurrencyState(
      last_min: Double
    )
    object ConcurrencyState {
      /** Starting state */
      def init: ConcurrencyState = ConcurrencyState(-1.0)
    }
  }

  protected case class ConcurrentClump(evs: List[Model.PlayByPlayEvent])

  protected sealed trait PossessionStatus
  protected case object PossessionArrowSwitch extends PossessionStatus
  protected case object PossessionError extends PossessionStatus
  protected case object PossessionContinues extends PossessionStatus
  protected case object PossessionEnd extends PossessionStatus

  protected case class PossessionEvent(dir: Direction.Value) {
    /** The team in possession */
    object AttackingTeam {
      def unapply(x: Model.MiscGameEvent): Option[String] = x match {
        case Model.OtherTeamEvent(_, _, _, event_str) if dir == Direction.Team => Some(event_str)
        case Model.OtherOpponentEvent(_, _, _, event_str) if dir == Direction.Opponent => Some(event_str)
        case _ => None
      }
    }
    /** The team not in possession */
    object DefendingTeam {
      def unapply(x: Model.MiscGameEvent): Option[String] = x match {
        case Model.OtherTeamEvent(_, _, _, event_str) if dir == Direction.Opponent => Some(event_str)
        case Model.OtherOpponentEvent(_, _, _, event_str) if dir == Direction.Team => Some(event_str)
        case _ => None
      }
    }
  }

/**/
case class StatsFragment(
  shots_made_or_missed: Int = 0,
  total_number_of_orbs: Int = 0,
  total_number_of_ft_events: Int = 0,
  total_number_of_tos: Int = 0
) {
  def sum(rhs: StatsFragment) = this.copy(
    shots_made_or_missed = this.shots_made_or_missed + rhs.shots_made_or_missed,
    total_number_of_orbs = this.total_number_of_orbs + rhs.total_number_of_orbs,
    total_number_of_ft_events = this.total_number_of_ft_events + rhs.total_number_of_ft_events,
    total_number_of_tos = this.total_number_of_tos + rhs.total_number_of_tos,
  )
  def total_poss = {
    shots_made_or_missed - total_number_of_orbs + total_number_of_ft_events + total_number_of_tos
  }
  def summary: String = {
    s"${shots_made_or_missed} - ${total_number_of_orbs} + ${total_number_of_ft_events} + ${total_number_of_tos} = $total_poss"
  }
}
  def calculate_stats(clump: List[Model.PlayByPlayEvent], dir: Direction.Value): StatsFragment = {
    val attackingTeam = PossessionEvent(dir).AttackingTeam

    val ft_event_this_clump = clump.collect {
      case attackingTeam(EventUtils.ParseFreeThrowMade(_)) => ()
      case attackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ()
    }.nonEmpty
    val and_one = clump.collect {
      case attackingTeam(EventUtils.ParseFreeThrowMade(_)) => ()
      case attackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ()
    }.size == 1

    val filtered_clump = clump.filter {
      case attackingTeam(EventUtils.ParseTeamDeadballRebound(_)) => false
      case _ => true
    }
    // Shots made or missed
    val shots_made_or_missed = filtered_clump.collect {
      case attackingTeam(EventUtils.ParseShotMade(_)) => ()
      case attackingTeam(EventUtils.ParseShotMissed(_)) => ()
    }.size
    val total_number_of_ft_events = if (ft_event_this_clump && !and_one) 1 else 0
    val total_number_of_orbs = filtered_clump.collect {
      case attackingTeam(EventUtils.ParseOffensiveRebound(_)) => ()
    }.size
    val total_number_of_tos = filtered_clump.collect {
      case attackingTeam(EventUtils.ParseTurnover(_)) => ()
    }.size
    StatsFragment(shots_made_or_missed, total_number_of_orbs, total_number_of_ft_events, total_number_of_tos)
  }

  /** We generate this if the possession calcuations go wrong */
  val error_event_string = "MISSING_POSSESSION_END"

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
    val raw_list =
      reverse_clump.foldLeft(List[Model.PlayByPlayEvent]()) { (acc, v) => v.evs ++ acc } //(re-reverses it)

    // In practice you can have possessions in both directions with concurrent clumps
    // This is hard to handle. For now we'll support 1 in each direction
    //TODO(support more)
    // By splitting the clump into (at most) 2, 1 for each direction based on analyzing the
    // event type (and using the prev/next if the event isn't clearly in one direction or the other)

    /** Example:
     RawGameEvent(None, Some("12:37:00,11-13,Dan Dwyer, rebound defensive"),
     RawGameEvent(None, Some("12:16:00,14-13,Justin Wright-Foreman, 3pt jumpshot made"),
     RawGameEvent(None, Some("12:16:00,14-13,Dan Dwyer, assist"),
     RawGameEvent(None, Some("12:16:00,14-13,Dan Dwyer, rebound defensive"),
     RawGameEvent(Some("12:16:00,14-13,Aaron Wiggins, 2pt layup blocked missed"),
     RawGameEvent(None, Some("12:16:00,14-13,Desure Buie, block"),
    */
//TODO: this will break offsetting fouls

    case class InClumpState(
      start: List[Model.PlayByPlayEvent], //these are consistent with the current direction
      end: List[Model.PlayByPlayEvent], //these are consistent with the switched direction
      prev: Direction.Value)

    val current_dir = s.direction
    val next_dir = Direction.opposite(current_dir)

    val attackingTeam = PossessionEvent(current_dir).AttackingTeam
    val defendingTeam = PossessionEvent(current_dir).DefendingTeam

    def get_direction(ev: Model.PlayByPlayEvent): Direction.Value = ev match {
      case attackingTeam(EventUtils.ParseOffensiveEvent(_)) =>
        current_dir
      case defendingTeam(EventUtils.ParseDefensiveEvent(_)) =>
        current_dir
      case defendingTeam(EventUtils.ParseOffensiveEvent(_)) =>
        next_dir
      case attackingTeam(EventUtils.ParseDefensiveEvent(_)) =>
        next_dir
      case _ => Direction.Init
    }

    val InClumpState(start_evs, end_evs, _) =
      raw_list.foldLeft(InClumpState(Nil, Nil, Direction.Init)) { (state, ev) =>
        get_direction(ev) match {
          // Known direction
          case `current_dir` =>
            state.copy(start = ev :: state.start, prev = current_dir)
          case `next_dir` =>
            state.copy(end = ev :: state.end, prev = next_dir)

          // Unknown direction, can infer from previous:
          case _ if state.prev == current_dir =>
            state.copy(start = ev :: state.start)
          case _ if state.prev == next_dir =>
            state.copy(end = ev :: state.end)

          // Unknown direction, no previous .. will fetch from next:
          case _ =>
            raw_list
              .toStream.map(get_direction)
              .filter(_ != Direction.Init)
              .headOption match {
                case Some(`current_dir`) =>
                  state.copy(start = ev :: state.start, prev = current_dir)
                case Some(_) => //(next dir)
                  state.copy(end = ev :: state.end, prev = next_dir)
                case None => //(default to current direction)
                  state.copy(start = ev :: state.start, prev = current_dir)
              }
        }
      }

/**
    List(
      ConcurrentClump(
        start_evs.reverse
      ),
      ConcurrentClump(
        end_evs.reverse
      )
    ).filter(_.evs.nonEmpty)
*/
List(ConcurrentClump(
  raw_list
))
  }
//TODO: to test

  /** Manages splitting stream into concurrent chunks and then combining them */
  protected val concurrent_event_handler = Clumper(
    PossState.ConcurrencyState.init,
    check_for_concurrent_event _,
    rearrange_concurrent_event _
  )

  /** Figure out who has possession to start the game */
  protected def first_possession_status(evs: List[Model.PlayByPlayEvent]): Option[Direction.Value] = {
    evs.collect {
      // Take advantage of new format
      case Model.OtherTeamEvent(_, _, _, EventUtils.ParseJumpballWon(_)) =>
        Direction.Team
      case Model.OtherOpponentEvent(_, _, _, EventUtils.ParseJumpballWon(_)) =>
        Direction.Opponent
      // Legacy calcuations:
      case Model.OtherTeamEvent(_, _, _, EventUtils.ParseOffensiveEvent(_)) =>
        Direction.Team
      case Model.OtherTeamEvent(_, _, _, EventUtils.ParseDefensiveActionEvent(_)) =>
        Direction.Opponent
      case Model.OtherOpponentEvent(_, _, _, EventUtils.ParseOffensiveEvent(_)) =>
        Direction.Opponent
      case Model.OtherOpponentEvent(_, _, _, EventUtils.ParseDefensiveActionEvent(_)) =>
        Direction.Team
      // Note have to ignore fouls since they can be offensive or defensive, will handle that
      // in some separate logic before the main fold, below
    }.headOption
  }

  /** Process the events within a concurrent clump to see if the possession is changing/has changed
   * Note that the clump has been split into "attacking" and "defending" events, so you won't ever
   * see both in the same clump
   */
  protected def clump_possession_status(
    state: PossState,
    in_evs: List[Model.PlayByPlayEvent]
  ): PossessionStatus =
  {
    val defendingTeam = PossessionEvent(state.direction).DefendingTeam
    val attackingTeam = PossessionEvent(state.direction).AttackingTeam

    /** Ignore any events that have already been processed, plus timeouts */
    def to_ignore: Model.PlayByPlayEvent => Boolean = {
      case ev: Model.MiscGameEvent if ev.poss > 0 => true
      case attackingTeam(EventUtils.ParseTimeout(_)) => true
      case defendingTeam(EventUtils.ParseTimeout(_)) => true
        //(timeouts have no bearing on possession)
      case _ => false
    }

    /** Error out if the possession information is inconsistent with events */
    def invalid_possession_state(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseDefensiveActionEvent(_)) => ()
      case defendingTeam(EventUtils.ParseOffensiveEvent(_)) => ()
    }.nonEmpty

    /** This is the highest prio .. if the defending team rebounds it, they now have possession
     * (note fouls on the defense following a missed shot/rebound do generate DRB events)
    */
    def defensive_rebound(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case defendingTeam(EventUtils.ParseRebound(_)) => ()
//TODO: deadball rebound as well, I think?
    }.nonEmpty

    /** Offensive turnover */
    def offensive_turnover(evs: List[Model.PlayByPlayEvent]): Boolean = evs.collect {
      case attackingTeam(EventUtils.ParseTurnover(_)) => ()
      case attackingTeam(EventUtils.ParsePersonalFoul(_)) if evs.collect {
        // Look out for offsetting personals:
        /** Example
        RawGameEvent(Some("07:43:00,51-61,Aaron Wiggins, rebound defensive"), None, Some(57), None),
        RawGameEvent(None, Some("07:28:00,51-61,Kevin Anderson, foulon"), None, Some(57)),
        RawGameEvent(Some("07:28:00,51-61,Ivan Bender, foul personal flagrant1"), None, Some(57), None),
        RawGameEvent(Some("07:28:00,51-61,Ivan Bender, foulon"), None, Some(57), None),
        RawGameEvent(None, Some("07:28:00,51-61,Kevin Anderson, foul personal flagrant1"), None, Some(57)),
        RawGameEvent(Some("07:10:00,51-61,Anthony Cowan, 2pt jumpshot missed"), None, Some(58), None),
        */
        case defendingTeam(EventUtils.ParsePersonalFoul(_)) => ()
      }.isEmpty => ()
//TODO: test
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
      }.sortBy(ev => Game.Score.unapply(ev.score)).reverse match { // reverse => highest first
        case attackingTeam(EventUtils.ParseFreeThrowMade(_)) :: _ => //last free throw was made
          PossessionEnd
        case attackingTeam(EventUtils.ParseFreeThrowMissed(_)) :: _ => //last free throw was missed
          PossessionContinues
      }
    }

    in_evs.filterNot(to_ignore) match {
      case evs if end_of_period(evs) => PossessionArrowSwitch

      case evs if defensive_rebound(evs) => PossessionEnd
        // (note: defensive rebounds can be concurrent with misses)

      case evs if invalid_possession_state(evs) => PossessionError
        //(it's a bit hard to parse out clumps with DRBs so we'll ignore them for invalid state checks)

      case evs if offensive_turnover(evs) => PossessionEnd
        //(if we see offensive moves from both teams in the same clump that's OK if there's
        // a possession change in that clump)

      case evs if technical_foul_defense(evs) => PossessionContinues
        //(if the defending team is called for a technical, the offensive team will get the ball back,
        // offensive technical fouls that result in a change of possession also generate an
        // offensive turnover event, so can ignore)

      case evs if made_shot(evs) && missed_and_one(evs) => PossessionContinues //(wait for the rebound)
        //(any free throws must be an and-1 because technical fouls are handled above)
      case evs if made_shot(evs) => PossessionEnd //(no and-1 because of order)

      case evs if fts_some_missed(evs) => handle_missed_freethrows(evs) //untangle what happened
      case evs if fts_some_made(evs) => PossessionEnd //(none missed because of order)

      case _ => PossessionContinues
    }
  }

  /** Calculate the possession counts within a lineup */
  def sum_possessions(curr: LineupEvent, prevs: List[LineupEvent]): (Int, Int) = {
    def team_or_oppo_calc(poss_getter: LineupEvent.RawGameEvent => List[Int]): Int = {
      val max_poss = Try(Some(curr.raw_game_events.flatMap(poss_getter).max)).getOrElse(None)
      val min_poss = Try(Some(curr.raw_game_events.flatMap(poss_getter).min)).getOrElse(None)
      val prev = prevs.iterator.find(p => p.raw_game_events.flatMap(poss_getter).nonEmpty)
      //(find most recent previous lineup even with a possession count)
      val prev_poss = prev.flatMap(
        p => Try(Some(p.raw_game_events.flatMap(poss_getter).max)).getOrElse(None)
      )
      (max_poss, min_poss, prev_poss) match {
        case (Some(max), Some(min), Some(prev)) if prev == min =>
          max - min //(min was already counted in the previous lineup)
        case (Some(max), Some(min), _) => max - min + 1
        case _ => 0
      }
    }
    (
      team_or_oppo_calc(_.team_possession.toList),
      team_or_oppo_calc(_.opponent_possession.toList)
    )
  }
//TODO test

  /** Returns team and opponent possessions based on the raw event data */
  def calculate_possessions(
    raw_events: Seq[Model.PlayByPlayEvent]
  ): List[Model.PlayByPlayEvent] =
  {
    /** Adds possession count to raw event */
    def enrich(state: PossState, ev: Model.PlayByPlayEvent): Model.PlayByPlayEvent = {
      val attackingTeam = PossessionEvent(state.direction).AttackingTeam
      ev match {
        // Special case...  a defensive action from the previous possession:
        /** Example:
        RawGameEvent(Some("17:42,5-0,FERNANDO,BRUNO Defensive Rebound"),
        RawGameEvent(Some("17:32,5-0,FERNANDO,BRUNO Turnover"),
        */
        case game_ev @ attackingTeam(EventUtils.ParseDefensiveInfoEvent(_)) if game_ev.poss <= 0 =>
          game_ev.with_poss(state.direction match {
            case Direction.Team => state.opponent
            case Direction.Opponent => state.team
            case _ => 0
          })
        case game_ev: Model.MiscGameEvent if game_ev.poss <= 0 =>
  //TODO: oh wait this isn't right, you can have opponent actions as part of team possession etc
          game_ev.with_poss(state.direction match {
            case Direction.Team => state.team
            case Direction.Opponent => state.opponent
            case _ => 0
          })
        case _ => ev
          //(never overwrite a previous possession, ie ev_poss > 0)
      }
    }

    /** Sort out state if possession changes */
    def switch_state(state: PossState): PossState = {
      if (state.direction == Direction.Team) {
        state.copy(
          opponent = state.opponent + 1, direction = Direction.Opponent
        )
      } else { //(must be Opponent, not Init, by construction)
        state.copy(
          team = state.team + 1, direction = Direction.Team
        )
      }
    }

    /** Sort out state if possession changes _across game break_ */
    def switch_state_on_game_break(state: PossState): PossState = {
      /** Examples
         T: made: 1
         O: made: 1 (here, state=(T: 2, O: 1, dir=T))
         <break, dir arrow = O|T>
         O: made: 2 | T: made: 2
         T: made: 2 | O: made: 2
         (note every half ends on a made shot or a -possibly deadball? defensive rebound)
      */
      if (state.direction == state.possession_arrow) {
        state
      } else {
        if (state.direction == Direction.Team) {
          state.copy(
            team = state.team - 1, opponent = state.opponent + 1, direction = Direction.Opponent
          )
        } else { //(must be Opponent, not Init, by construction)
          state.copy(
            team = state.team + 1, opponent = state.opponent - 1, direction = Direction.Team
          )
        }
      }
    }

    /** If a possession has been missed, create a dummy one to keep the stats correct */
    def inject_missed_possession(evs: List[Model.PlayByPlayEvent], dir: Direction.Value): Model.PlayByPlayEvent = {
      /** Example of a missed possession:
      RawGameEvent(Some("13:23:00,8-13,Bruno Fernando, foul personal shooting;2freethrow"),
      RawGameEvent(None, Some("13:22:00,9-13,Stafford Trueheart, freethrow 2of2 2ndchance made"),
      RawGameEvent(None, Some("13:22:00,8-13,Stafford Trueheart, freethrow 1of2 2ndchance missed"),
      RawGameEvent(None, Some("13:22:00,8-13,Team, rebound offensivedeadball"),
      RawGameEvent(Some("12:50:00,11-13, MISSING_POSSESSION"),
      RawGameEvent(None, Some("12:50:00,11-13,Justin Wright-Foreman, 2pt layup pointsinthepaint made"),
      */

      val first_game_event = evs.collect {
        case ev: Model.MiscGameEvent => ev
      }.headOption.getOrElse(Model.OtherTeamEvent(0.0, Game.Score(0, 0), 0, ""))
      val event_info = first_game_event.event_string.split(",", 3)
      val event_time = event_info.lift(0).getOrElse("00:00")
      val event_score = event_info.lift(1).getOrElse("0-0")
      val injected_ev_str = s"${event_time},${event_score}, $error_event_string"
      dir match { //(possession gets injected)
        case Direction.Team => Model.OtherTeamEvent(first_game_event.min, first_game_event.score, 0, injected_ev_str)
        case _ => Model.OtherOpponentEvent(first_game_event.min, first_game_event.score, 0, injected_ev_str)
      }
    }

    /** Updates the state and enriches the events with poss number based on incoming clump */
    def handle_clump(
      state: PossState, evs: List[Model.PlayByPlayEvent]
    ): (PossState, List[Model.PlayByPlayEvent]) = clump_possession_status(state, evs) match {

      case PossessionError => //create a missed possession state and carry on
        val dummy_event = enrich(state, inject_missed_possession(evs, state.direction))
        val new_state = switch_state(state)
        //(this can't recurse because each clump is guaranteeed to match one direction only)
        handle_clump(new_state, dummy_event :: evs)

      case PossessionArrowSwitch =>
        val new_state = switch_state_on_game_break(state).copy(
          possession_arrow = Direction.opposite(state.possession_arrow)
        )
        //Game break meaning whoever has the possession arrow gets the ball, so may or may not`
        //get consecutive possessions on either side
        (new_state, evs.map(ev => enrich(state, ev)))

      case PossessionEnd =>
        val new_state = switch_state(state)
        (new_state, evs.map(ev => enrich(state, ev)))

      case PossessionContinues =>
        (state, evs.map(ev => enrich(state, ev)))
    }

//TODO: before we do anything else, let's check if the first possession is "difficult" and inject
//dummy events to make life easier if so

    val clumped_events = raw_events.map(ev => ConcurrentClump(ev :: Nil))
    StateUtils.foldLeft(
      clumped_events, PossState.init, classOf[Model.PlayByPlayEvent], concurrent_event_handler
    ) {
      case StateEvent.Next(ctx, state, ConcurrentClump(evs)) =>
        ctx.stateChange(state.copy(
          team_stats = state.team_stats.sum(calculate_stats(evs, Direction.Team)),
          opponent_stats = state.opponent_stats.sum(calculate_stats(evs, Direction.Opponent))
        ))

      case StateEvent.Complete(ctx, state) => //(no additional processing when element list complete)
        println("--------------------")
        println(s"[${state.team_stats.summary}]")
        println(s"[${state.opponent_stats.summary}]")
        println("--------------------")
        ctx.noChange

    }
    raw_events.toList

    /**
    val clumped_events = raw_events.map(ev => ConcurrentClump(ev :: Nil))
    (StateUtils.foldLeft(
      clumped_events, PossState.init, classOf[Model.PlayByPlayEvent], concurrent_event_handler
    ) {
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
    */

  } //(end calculate_possessions)
}
object PossessionUtils extends PossessionUtils
