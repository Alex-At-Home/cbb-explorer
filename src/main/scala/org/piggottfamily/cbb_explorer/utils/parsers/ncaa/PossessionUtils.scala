package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import scala.util.Try

/** Utilities related to calculation possession from raw game events */
trait PossessionUtils {
  import ExtractorUtils._
  import StateUtils.StateTypes._

  /** Useful scriptlet for checking results
  // Show results
  l.groupBy(t => (t.opponent, t.location_type)).mapValues(
    _.foldLeft((0,0))
    { (acc, v) => (acc._1 + v.team_stats.num_possessions,acc._2 + v.opponent_stats.num_possessions) }
  )
  // Compare to previous results
  (x1 zip x2).map { case (k, v) => k._1 -> (k._2._1 - v._2._1, k._2._2 - v._2._2) }
  */

  // Lots of data modelling:

  /** Which team is in possession */
  protected object Direction extends Enumeration {
    val Init, Team, Opponent = Value
  }
  /** State for building possession events */
  protected case class PossState(
    team_stats: PossCalcFragment,
    opponent_stats: PossCalcFragment,
    prev_clump: ConcurrentClump
  )
  protected object PossState {
    /** Starting state */
    def init: PossState  = PossState(
      PossCalcFragment(), PossCalcFragment(), ConcurrentClump(Nil)
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

  protected case class ConcurrentClump(evs: List[LineupEvent.RawGameEvent]) {
    def min: Option[Double] = evs.headOption.map(_.min)
    def date_str: Option[String] = evs.headOption.map(_.date_str)
  }
  protected case class Last2Clumps(prev: ConcurrentClump, prevprev: ConcurrentClump)

  protected case class PossessionEvent(dir: Direction.Value) {
    /** The team in possession */
    object AttackingTeam {
      def unapply(x: LineupEvent.RawGameEvent): Option[String] = x match {
        case LineupEvent.RawGameEvent.Team(event_str) if dir == Direction.Team => Some(event_str)
        case LineupEvent.RawGameEvent.Opponent(event_str) if dir == Direction.Opponent => Some(event_str)
        case _ => None
      }
    }
    /** The team not in possession */
    object DefendingTeam {
      def unapply(x: LineupEvent.RawGameEvent): Option[String] = x match {
        case LineupEvent.RawGameEvent.Team(event_str) if dir == Direction.Opponent => Some(event_str)
        case LineupEvent.RawGameEvent.Opponent(event_str) if dir == Direction.Team => Some(event_str)
        case _ => None
      }
    }
  }

  /** Maintains stats needed to calculate possessions for each lineup event */
  case class PossCalcFragment(
    shots_made_or_missed: Int = 0,
    liveball_orbs: Int = 0,
    actual_deadball_orbs: Int = 0,
    ft_events: Int = 0,
    ignored_and_ones: Int = 0,
    bad_fouls: Int = 0,
    offsetting_bad_fouls: Int = 0,
    turnovers: Int = 0
  ) {
    def sum(rhs: PossCalcFragment) = this.copy(
      shots_made_or_missed = this.shots_made_or_missed + rhs.shots_made_or_missed,
      liveball_orbs = this.liveball_orbs + rhs.liveball_orbs,
      actual_deadball_orbs = this.actual_deadball_orbs + rhs.actual_deadball_orbs,
      ft_events = this.ft_events + rhs.ft_events,
      ignored_and_ones = this.ignored_and_ones + rhs.ignored_and_ones,
      bad_fouls = this.bad_fouls + rhs.bad_fouls,
      offsetting_bad_fouls = this.offsetting_bad_fouls + rhs.offsetting_bad_fouls,
      turnovers = this.turnovers + rhs.turnovers
    )
    def total_poss = {
      shots_made_or_missed - (liveball_orbs + actual_deadball_orbs) +
      (ft_events - bad_fouls) + turnovers
    }
    def summary: String = {
      s"total=[$total_poss] = " +
      s"shots=[$shots_made_or_missed] - (orbs=[$liveball_orbs] + db_orbs=[$actual_deadball_orbs]) + " +
      s"(ft_sets=[$ft_events] - techs=[$bad_fouls]) + to=[$turnovers]" +
      s" { +1s=[$ignored_and_ones] offset_techs=[$offsetting_bad_fouls] }"
    }
  }

  /** Decompose the lineup - resulting objects */
  protected case class LineupDecomp(
    lineup: LineupEvent, next_lineup: Option[LineupEvent], reverse_prev_lineups: List[LineupEvent]
  )

  /** Util methods */

  /** Calculates the possessions from a block of data (eg game or lineup)
      for one direction only. As stateless and independent in the 2 dirs as possible!

  Here's an example where we get it wrong:
    10:04:00     Kevin Anderson, 3pt jumpshot missed     42-58
  10:02:00     Eric Carter, freethrow 1of1 made     45-58
  10:02:00         44-58     Bruno Fernando, foul personal shooting;1freethrow
  10:02:00     Eric Carter, 2pt layup pointsinthepaint made     44-58
  10:02:00         42-58     Eric Ayala, rebound defensive
  10:02:00     Ryan Johnson, foulon     44-58
  ...there's _probably_ a missing turnover, don't think there's much we can do about it

  TODO: longer term need a context of nearby events, though this wouldn't have helped here
  */
  protected def calculate_stats(
    clump: ConcurrentClump, prev: ConcurrentClump, dir: Direction.Value
  ): PossCalcFragment = {
    val attackingTeam = PossessionEvent(dir).AttackingTeam
    val defendingTeam = PossessionEvent(dir).DefendingTeam

    /** Examples: have seen a spot when FTs aren't concurrent:
    11:02:00		45-37	Connor McCaffery, freethrow 1of2 2ndchance;fastbreak made
    10:56:00	Ricky Lindo Jr., substitution out	45-37
    10:56:00	Darryl Morsell, substitution in	45-37
    10:44:00		45-38	Connor McCaffery, freethrow 2of2 2ndchance;fastbreak made
//TODO: with new format can handle this by looking for 1of[23]
    */

    val ft_event_this_clump: Boolean = clump.evs.collect {
      case attackingTeam(EventUtils.ParseFreeThrowEvent(_)) => ()
    }.nonEmpty

    /** Examples:
    Actual and-one:
    19:10:00	Tyrone Lyons, 2pt jumpshot 2ndchance;fromturnover;pointsinthepaint made	27-49
    19:06:00		27-49	Bruno Fernando, foul personal shooting;1freethrow
    19:06:00	Tyrone Lyons, foulon	27-49
    19:06:00	Tyrone Lyons, freethrow 1of1 2ndchance;fastbreak;fromturnover made

    Event more confusing version:
    (i rearranged every line of the 1:33 section to make it somewhat readable!)
    01:36:00		68-60	Eric Ayala, 3pt jumpshot missed
    01:33:00	Jack Salt, rebound defensive	68-60
    01:33:00	Jack Salt, foulon	68-60
    01:33:00		68-60	Bruno Fernando, foul personal 1freethrow (<-this normally means and-1)
    01:33:00	Jack Salt, freethrow 1of1 fastbreak missed	68-60
    01:33:00		70-62	Jalen Smith, rebound defensive (<--THE SCORE IS EVEN WRONG HERE)
    01:33:00	De'Andre Hunter, 2pt dunk pointsinthepaint;fastbreak made	70-60
    01:33:00	Kyle Guy, assist	70-60
    01:33:00		70-62	Anthony Cowan, 2pt layup pointsinthepaint made

    One-and-one to confuse things:
    00:50:40	Ty Jerome, freethrow 1of1 fastbreak missed	70-65
    00:50:40		70-65	Bruno Fernando, rebound defensive
    00:50:40		70-65	Aaron Wiggins, foul personal oneandone

    The detection won't be perfect, we're going to look for a free throw that is
    (concurrent with a made shot) OR
    the previous clump had a made shot for direction AND not in the other direction
    */
    def combined_events_iterator = (clump.evs.iterator ++ prev.evs.iterator)
    val and_one: Int = if (
        (clump.evs.collect { //(there's exactly 1 FT)...
          case attackingTeam(EventUtils.ParseFreeThrowMade(_)) => ()
          case attackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ()
        }.size == 1) &&
          (clump.evs.collect { //...(either concurrent with a made shot)...
            case attackingTeam(EventUtils.ParseShotMade(_)) => ()
          }.nonEmpty ||
            (prev.evs.collect { //...(or the last clump, but...
              case attackingTeam(EventUtils.ParseShotMade(_)) => ()
            }.nonEmpty &&
            prev.evs.collect { // ... in that clump the opponent _didn't_ have possession)
              case defendingTeam(EventUtils.ParseOffensiveEvent(_)) => ()
            }.isEmpty)
          )
        ) 1 else 0

    val filtered_clump = clump.evs.filter {
      case attackingTeam(EventUtils.ParseDeadballRebound(_)) => false
      case _ => true
    }

    // Shots made or missed
    val shots_made_or_missed: Int = filtered_clump.collect {
      case attackingTeam(EventUtils.ParseShotMade(_)) => ()
      case attackingTeam(EventUtils.ParseShotMissed(_)) => ()
    }.size

    // Free throws
    val ft_event: Int = if (ft_event_this_clump && (and_one == 0)) 1 else 0

    /** Technicals and flagrants, examples:
      Technical:
        14:27:00	Eric Carter, 2pt layup pointsinthepaint made	36-56
        14:26:00	Eric Carter, foul technical classa;2freethrow	36-56
        14:26:00		36-58	Anthony Cowan, freethrow 2of2 fastbreak made
        14:26:00		36-57	Anthony Cowan, freethrow 1of2 fastbreak made
        14:06:00	Eric Carter, block	36-58
        14:06:00		36-58	Jalen Smith, 2pt layup blocked missed
      Flagrant:
        03:44:00	Ithiel Horton, 2pt layup missed	60-67
        03:42:00		62-67	Darryl Morsell, freethrow 1of2 fastbreak missed
        03:42:00		60-67	Aaron Wiggins, foulon
        03:42:00	Eric Carter, freethrow 1of2 made	61-67
        03:42:00	Eric Carter, freethrow 2of2 made	62-67
        03:42:00		62-67	Team, rebound offensivedeadball
        03:42:00		62-68	Darryl Morsell, freethrow 2of2 fastbreak made
        03:42:00	Eric Carter, foul personal flagrant1;2freethrow	60-67
        03:42:00		60-67	Darryl Morsell, substitution in
        03:42:00	timeout commercial
        03:42:00	Kevin Anderson, foulon	60-67
        03:42:00		60-67	Bruno Fernando, substitution out
        03:42:00		60-67	Bruno Fernando, foul personal oneandone
        03:42:00	Kevin Anderson, rebound offensive	60-67
        03:24:00		62-70	Eric Ayala, 2pt jumpshot made
      Offsetting:
        07:43:00		51-61	Aaron Wiggins, rebound defensive
        07:28:00	Kevin Anderson, foulon	51-61
        07:28:00		51-61	Ivan Bender, foul personal flagrant1
        07:28:00		51-61	Ivan Bender, foulon
        07:28:00	Kevin Anderson, foul personal flagrant1	51-61
        07:10:00		51-61	Anthony Cowan, 2pt jumpshot missed

        Not much that we can do with old format because fouls just appear as normal
    */

    val offsetting_tech: Int = if (filtered_clump.collect {
      case attackingTeam(EventUtils.ParseTechnicalFoul(_)) => ()
    }.nonEmpty && filtered_clump.collect {
      case defendingTeam(EventUtils.ParseTechnicalFoul(_)) => ()
    }.nonEmpty) 1 else 0

    val offsetting_flagrant: Int = if (filtered_clump.collect {
      case attackingTeam(EventUtils.ParseFlagrantFoul(_)) => ()
    }.nonEmpty && filtered_clump.collect {
      case defendingTeam(EventUtils.ParseFlagrantFoul(_)) => ()
    }.nonEmpty) 1 else 0

    val tech_or_flagrant: Int = (if (filtered_clump.collect {
      case defendingTeam(EventUtils.ParseTechnicalFoul(_)) => ()
      case defendingTeam(EventUtils.ParseFlagrantFoul(_)) => ()
    }.nonEmpty) 1 else 0) - offsetting_tech - offsetting_flagrant

    val orbs: Int = filtered_clump.collect {
      case attackingTeam(EventUtils.ParseOffensiveRebound(_)) => ()
    }.size

    /** Examples:
    Filter this out, but not because it's part of a FT even, it's at "end of period" artefact
    00:00:10		27-44	Jalen Smith, 3pt jumpshot fromturnover missed
    00:00:10		27-44	Team, rebound offensivedeadball

    Here's one we need to process though:
    10:02:00    Ryan Johnson, foulon    44-58
    09:51:00        45-58    Anthony Cowan, foulon
    09:51:00        45-58    Anthony Cowan, 3pt jumpshot missed
    09:51:00        45-58    Team, rebound offensivedeadball
    09:51:00    Ryan Johnson, foul personal    45-58
    09:51:00        45-58    Ricky Lindo Jr., substitution out
    09:51:00        45-58    Serrel Smith Jr., substitution out
    09:51:00        45-58    Anthony Cowan, substitution in
    09:51:00        45-58    Darryl Morsell, substitution in
    09:51:00        45-58    Bruno Fernando, substitution out
    09:51:00        45-58    Jalen Smith, substitution in
    09:51:00    Ryan Johnson, substitution out    45-58
    09:51:00    Darian Bryant, substitution in    45-58
    09:49:00        45-58    Jalen Smith, turnover badpass

    Ignorable rebounds Can also be in prev clump
    07:49:00		51-60	Darryl Morsell, freethrow 1of2 fastbreak;fromturnover missed
    ...
    07:45:00		51-61	Team, rebound offensivedeadball

    This won't be perfect, but we'll ignore any deadball rebounds if the current
    or previous clumps involved missed free throws
    (otherwise it gets complicated with techs and 1-and-1s)

    Also only works with new format
    */

    val recent_dead_ft_misses = List(clump.evs, prev.evs).map { evs =>
      evs.collect {
        case ev @ attackingTeam(EventUtils.ParseFreeThrowMade(_)) => ev
        case ev @ attackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ev
      }.sortBy(ev => ev.score_str).reverse match { // reverse => highest first
//TODO: this will be wrong when a FT moves from 9->10 or 99->100
        case fts => fts.drop(1).collect {
          case ev @ attackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ev
        }.size
      }
    }.sum

    // this can be wrong if the _prev_ clump had a technical/flagrant, we'll live with that
    val real_deadball_orbs =
      if ((recent_dead_ft_misses == 0) && (tech_or_flagrant == 0)) clump.evs.collect {
        case ev @ attackingTeam(EventUtils.ParseOffensiveDeadballRebound(_))
          if !ev.info.startsWith("00:00") //(see spurious deadball rebounds at the end of the period)
        => ()
      }.size else 0

    val turnovers = filtered_clump.collect {
      case attackingTeam(EventUtils.ParseTurnover(_)) => ()
    }.size

    PossCalcFragment(
      shots_made_or_missed,
      orbs, real_deadball_orbs,
      ft_event, and_one,
      tech_or_flagrant, offsetting_tech + offsetting_flagrant,
      turnovers
    )
  }

  /** Identify concurrent events (easy) */
  protected def check_for_concurrent_event[S](
    ev: Clumper.Event[S, PossState.ConcurrencyState, ConcurrentClump]
  ): (PossState.ConcurrencyState, Boolean) = ev match {
    case Clumper.Event(_, cs, Nil, ConcurrentClump(ev :: _)) =>
      //(first event always gens a clump, possibly of size 1)
      (cs.copy(last_min = ev.min), true)

    //case Clumper.Event(_, cs, _, ConcurrentClump((ev: Model.MiscGameBreak) :: _)) =>
      //(the equivalent to this doesn't exist in lineups)
      // Game breaks can never be part of a clump
      //(cs.copy(last_min = -1.0), false)
    case Clumper.Event(_, cs, _, ConcurrentClump(ev :: _)) if ev.min == cs.last_min =>
      (cs, true)
    case Clumper.Event(_, cs, _, ConcurrentClump(ev :: _)) =>
      (cs.copy(last_min = ev.min), false)
    case Clumper.Event(_, cs, _, ConcurrentClump(Nil)) => //(empty ConcurrentClump, not possible by construction)
      (cs, true)
  }

  /** Aggregates all concurrent clumps of 1 event into a single clump of many events */
  protected def rearrange_concurrent_event[S](
    s: S, cs: PossState.ConcurrencyState, reverse_clump: List[ConcurrentClump]
  ): List[ConcurrentClump] = {
    val raw_list =
      reverse_clump.foldLeft(List[LineupEvent.RawGameEvent]()) { (acc, v) => v.evs ++ acc } //(re-reverses it)
    List(ConcurrentClump(
      raw_list
    ))
  }

  /** Manages splitting stream into concurrent chunks and then combining them */
  protected def concurrent_event_handler[S] = Clumper(
    PossState.ConcurrencyState.init,
    check_for_concurrent_event[S] _,
    rearrange_concurrent_event[S] _
  )

  /** Debug flag */
  protected val show_end_of_raw_calcs = false
  protected val show_end_of_lineup_calcs = true

  /** Debug util */
  protected def summarize_state(label: String, state: PossState): Unit = {
    println(s"--------$label------------")
    println(s"[${state.team_stats.summary}]")
    println(s"[${state.opponent_stats.summary}]")
    println("-------------------------")
  }

  /** Returns team and opponent possessions based on the lineup data */
  def calculate_possessions(
    lineup_events: Seq[LineupEvent]
  ): List[LineupEvent] = {

    var diagnostic_state = PossState.init

    val enriched_lineups = decompose_lineups(lineup_events).map {
      case LineupDecomp(lineup, next_lineup, reverse_prev_lineups) =>
        val start_of_next =
          get_first_clump(next_lineup.map(_.raw_game_events.filter {
            case LineupEvent.RawGameEvent.Opponent(EventUtils.ParseTeamSubIn(_)) => false
            case LineupEvent.RawGameEvent.Opponent(EventUtils.ParseTeamSubOut(_)) => false
            case _ => true
          }).getOrElse(Nil))

        val Last2Clumps(end_of_curr, _) = get_last_clumps(lineup.raw_game_events)
        val start_of_curr = get_first_clump(lineup.raw_game_events)
        val Last2Clumps(prev, prevprev) = get_last_clumps(reverse_prev_lineups.flatMap(_.raw_game_events))

        def half_time_boundary(later: Option[String], earlier: Option[String]): Boolean =
          later.flatMap(l => earlier.map(e => (e, l))).exists(e_l => e_l._1 > e_l._2)

        val (team_fragment, opponent_fragment) = (lineup.raw_game_events match { //(filter the end of the lineup)
          case l if half_time_boundary(start_of_next.date_str, end_of_curr.date_str) => //half time!
            l
          case l if start_of_next.min.nonEmpty && (start_of_next.min == end_of_curr.min) =>
            // Exclude the final concurrent clump because we handled it last iteration
            val min_to_exclude = start_of_next.min.getOrElse(-1.0)
/**///TODO: can this actually happen? I don't _think_ so
            if (prev.min == min_to_exclude) {
              println(s">>>>>>>>>>>>>> problem case: [$start_of_next][$end_of_curr][$start_of_next][$prev]")
            }
            l.filterNot(_.min == min_to_exclude)
          case l =>
            l
        }) match { //(pick the right bits of curr/prev and calculate possessions)
          case l if half_time_boundary(start_of_curr.date_str, prev.date_str) => //half time!
            calculate_possessions(l, Nil) //TODO: need to clear prevprev?
          case l if start_of_curr.min.nonEmpty && (start_of_curr.min == prev.min) =>
            calculate_possessions(prev.evs ++ l, prevprev.evs)
          case l =>
            calculate_possessions(l, prev.evs)
        }
//TODO: this is still wrong :(

        // Validation and stats collection

        diagnostic_state = diagnostic_state.copy(
          team_stats = diagnostic_state.team_stats.sum(team_fragment),
          opponent_stats = diagnostic_state.opponent_stats.sum(opponent_fragment)
        )

        //Validate no nasty lineup stats:
        def validate_lineup(fragment: PossCalcFragment, stats: LineupEventStats): Unit =
          if ((fragment.total_poss < 0) || ((fragment.total_poss == 0) && (stats.pts > 0))) {
            println(
              s"Possession problem: [${fragment.summary}] vs [$start_of_next] >> [$lineup] >> [$prev] >> [$prevprev]"
            )
          }
//TODO: example of lineup getting its possession stolen:
//Possession problem: [total=[-1] = shots=[2] - (orbs=[3] + db_orbs=[0]) + (ft_sets=[0] - techs=[0]) + to=[0] { +1s=[0] offset_techs=[0] }]
// next (t1): foul, FTM, FTm, ORB, make
// curr (t2->t3): (t)ORB, FGm, foul, ORB, FGm, ORB
//(ie next+curr) would be 1 ft_set, 4 ORBs, 1 make, 2 misses == 0 possessions
//(confusing because of ORB to start the lineup, but don't have a real val for prev lineup so....)
//TOOD: check for "10:08:00,13-22,Team, rebound offensive team" in NCAT game
// prev (t4): turnover
// Extreme oddity: t1->t3 == 10.28->9.87 ... BUT t4==3.47, so clearly (seperate) bug there

//TODO: need to decide what I mean by a possession eg in the above case, L1 and L2 legit are using the same
// L1 has no conclusion... prob L1 should be (0, 0) and L2 should be (1, 0)?
// (note that this might be differnet in the two different directions)

        validate_lineup(team_fragment, lineup.team_stats)
        validate_lineup(opponent_fragment, lineup.opponent_stats)

        // Possession-enriched lineup data

        lineup.copy(
          team_stats = lineup.team_stats.copy(
            num_possessions = team_fragment.total_poss
          ),
          opponent_stats = lineup.opponent_stats.copy(
            num_possessions = opponent_fragment.total_poss
          )
        )
    }
    if (show_end_of_lineup_calcs) {
      summarize_state("-END-", diagnostic_state)
    }
    enriched_lineups.toList
  }
//TODO TOTEST

  /** Returns team and opponent possessions based on the raw event data */
  protected def calculate_possessions(
    raw_events: Seq[LineupEvent.RawGameEvent],
    previous_events: Seq[LineupEvent.RawGameEvent]
  ): (PossCalcFragment, PossCalcFragment) =
  {

    val clumped_events = raw_events.map(ev => ConcurrentClump(ev :: Nil))
    StateUtils.foldLeft(
      clumped_events, PossState.init,
      classOf[LineupEvent.RawGameEvent], concurrent_event_handler[PossState]
    ) {
      case StateEvent.Next(ctx, state, clump @ ConcurrentClump(evs)) =>
        val new_state = state.copy(
          team_stats = state.team_stats.sum(
            calculate_stats(clump, state.prev_clump, Direction.Team)
          ),
          opponent_stats = state.opponent_stats.sum(
            calculate_stats(clump, state.prev_clump, Direction.Opponent)
          ),
          prev_clump = clump
        )
        ctx.stateChange(new_state)

      case StateEvent.Complete(ctx, state) => //(no additional processing when element list complete)
        if (show_end_of_raw_calcs) {
          summarize_state("-END-", state)
        }
        ctx.noChange

    } match {
      case FoldStateComplete.State(state) => (state.team_stats, state.opponent_stats)
    }

  } //(end calculate_possessions)

  /** Gets the first clump of a lineup (to compare with the "previous" in possession calcs) */
  protected def get_first_clump(evs: Seq[LineupEvent.RawGameEvent]): ConcurrentClump = {
    val ev_clumps = evs.map(ev => ConcurrentClump(List(ev)))
    StateUtils.foldLeft(
      ev_clumps, ConcurrentClump(Nil), concurrent_event_handler[ConcurrentClump]
    ) {
      case StateEvent.Next(ctx, state, clump) if state.evs == Nil =>
        ctx.stateChange(clump)
      case StateEvent.Next(ctx, _, _)  =>
        ctx.noChange
      case StateEvent.Complete(ctx, _) =>
        ctx.noChange
    } match {
      case FoldStateComplete.State(state) => state
    }
  }
//TODO TOTEST

  /** Gets the final clump of a lineup (to use as the "previous" in possession calcs) */
  protected def get_last_clumps(evs: Seq[LineupEvent.RawGameEvent]): Last2Clumps = {
    val start_state = Last2Clumps(ConcurrentClump(Nil), ConcurrentClump(Nil))
    val ev_clumps = evs.map(ev => ConcurrentClump(List(ev)))
    StateUtils.foldLeft(
      ev_clumps, start_state, concurrent_event_handler[Last2Clumps]
    ) {
      case StateEvent.Next(ctx, state, clump) =>
        ctx.stateChange(state.copy(prev = clump, prevprev = state.prev))
      case StateEvent.Complete(ctx, _) =>
        ctx.noChange
    } match {
      case FoldStateComplete.State(state) => state
    }
  }
//TODO TOTEST

  /** Decompose the lineup - resulting objects */
  protected def decompose_lineups(evs: Seq[LineupEvent]): Seq[LineupDecomp] = {
    //ie (1, Some(2)), (2, Some(3)), (N, None)
    val start_state: LineupDecomp = null
    (evs zip (evs.drop(1).map(Some(_)) ++ List(None))).scanLeft(start_state) {
      case (`start_state`, (ev, next_ev)) =>
        LineupDecomp(ev, next_ev, Nil)
      case (LineupDecomp(curr, _, reverse_prevs), (ev, next_ev)) =>
        LineupDecomp(ev, next_ev, curr :: reverse_prevs)
        //(generates LineupDecomp(1, Some(2), nil), LineupDecom(2, Some(3), L(1)), etc)
    }.drop(1) //ignore starting state
  }
//TODO TOTEST


}
object PossessionUtils extends PossessionUtils
