package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import scala.util.Try

import com.softwaremill.quicklens._

/** Utilities related to calculation possession from raw game events */
trait PossessionUtils {
  import ExtractorUtils._
  import StateUtils.StateTypes._
  //(these used to live in here but moved them centrally)
  import org.piggottfamily.cbb_explorer.models.ncaa.LineupEvent.RawGameEvent.Direction
  import org.piggottfamily.cbb_explorer.models.ncaa.LineupEvent.RawGameEvent.PossessionEvent

  /** Debug flags */
  protected val show_end_of_raw_calcs = true
  protected val log_lineup_fixes = true

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
      last_min: Double,
      last_date_str: String //(for game break detection)
    )
    object ConcurrencyState {
      /** Starting state */
      def init: ConcurrencyState = ConcurrencyState(-1.0, "")
    }
  }

  /** A clump of concurrent events, together with the lineups that end in that clump */
  protected case class ConcurrentClump(
    evs: List[LineupEvent.RawGameEvent], lineups: List[LineupEvent] = Nil
  ) {
    def min: Option[Double] = evs.headOption.map(_.min)
    def date_str: Option[String] = evs.headOption.map(_.date_str)
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

  /** Util methods */

  /** Identify concurrent events (easy) */
  protected def check_for_concurrent_event[S](
    ev: Clumper.Event[S, PossState.ConcurrencyState, ConcurrentClump]
  ): (PossState.ConcurrencyState, Boolean) = ev match {
    case Clumper.Event(_, cs, Nil, ConcurrentClump(ev :: _, _)) =>
      //(first event always gens a clump, possibly of size 1)
      (cs.copy(last_min = ev.min, last_date_str = ev.date_str), true)

    case Clumper.Event(_, cs, _, ConcurrentClump(Nil, _ :: _)) =>
      // Lineup change -  lineups just get added to the list
      (cs, true)

    case Clumper.Event(_, cs, _, ConcurrentClump(ev :: _, _)) if ev.date_str > cs.last_date_str =>
      // Game break
      (cs.copy(last_min = -1.0, last_date_str = ev.date_str), false)

    case Clumper.Event(_, cs, _, ConcurrentClump(ev :: _, _)) if ev.min == cs.last_min =>
      (cs, true)
    case Clumper.Event(_, cs, _, ConcurrentClump(ev :: _, _)) =>
      (cs.copy(last_min = ev.min, last_date_str = ev.date_str), false)
    case Clumper.Event(_, cs, _, ConcurrentClump(Nil, Nil)) => //(empty ConcurrentClump, not possible by construction)
      (cs, true)
  }

  /** Aggregates all concurrent clumps of 1 event into a single clump of many events */
  protected def rearrange_concurrent_event[S](
    s: S, cs: PossState.ConcurrencyState, reverse_clump: List[ConcurrentClump]
  ): List[ConcurrentClump] = {
    List(
      reverse_clump.foldLeft(ConcurrentClump(Nil, Nil)) { (acc, v) =>
        acc.copy(evs = v.evs ++ acc.evs, lineups = v.lineups ++ acc.lineups)
      } //(re-reverses the lists)
    )
  }

  /** Manages splitting stream into concurrent chunks and then combining them */
  protected def concurrent_event_handler[S] = Clumper(
    PossState.ConcurrencyState.init,
    check_for_concurrent_event[S] _,
    rearrange_concurrent_event[S] _
  )

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
  */
  protected def calculate_stats(
    clump: ConcurrentClump, prev: ConcurrentClump, dir: Direction.Value
  ): PossCalcFragment = {
    val attacking_team = PossessionEvent(dir).AttackingTeam
    val defending_team = PossessionEvent(dir).DefendingTeam

    /** Examples: have seen a spot when FTs aren't concurrent:
    11:02:00		45-37	Connor McCaffery, freethrow 1of2 2ndchance;fastbreak made
    10:56:00	Ricky Lindo Jr., substitution out	45-37
    10:56:00	Darryl Morsell, substitution in	45-37
    10:44:00		45-38	Connor McCaffery, freethrow 2of2 2ndchance;fastbreak made
    */

    val ft_event_this_clump: Boolean = clump.evs.collect {
      case attacking_team(EventUtils.ParseFreeThrowEvent(_)) => ()
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
          case attacking_team(EventUtils.ParseFreeThrowMade(_)) => ()
          case attacking_team(EventUtils.ParseFreeThrowMissed(_)) => ()
        }.size == 1) &&
          (clump.evs.collect { //...(either concurrent with a made shot)...
            case attacking_team(EventUtils.ParseShotMade(_)) => ()
          }.nonEmpty ||
            (prev.evs.collect { //...(or the last clump, but...
              case attacking_team(EventUtils.ParseShotMade(_)) => ()
            }.nonEmpty &&
            prev.evs.collect { // ... in that clump the opponent _didn't_ have possession)
              case defending_team(EventUtils.ParseOffensiveEvent(_)) => ()
            }.isEmpty)
          )
        ) 1 else 0

    val filtered_clump = clump.evs.filter {
      case attacking_team(EventUtils.ParseDeadballRebound(_)) => false
      case _ => true
    }

    // Shots made or missed
    val shots_made_or_missed: Int = filtered_clump.collect {
      case attacking_team(EventUtils.ParseShotMade(_)) => ()
      case attacking_team(EventUtils.ParseShotMissed(_)) => ()
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
      case attacking_team(EventUtils.ParseTechnicalFoul(_)) => ()
    }.nonEmpty && filtered_clump.collect {
      case defending_team(EventUtils.ParseTechnicalFoul(_)) => ()
    }.nonEmpty) 1 else 0

    val offsetting_flagrant: Int = if (filtered_clump.collect {
      case attacking_team(EventUtils.ParseFlagrantFoul(_)) => ()
    }.nonEmpty && filtered_clump.collect {
      case defending_team(EventUtils.ParseFlagrantFoul(_)) => ()
    }.nonEmpty) 1 else 0

    val offsetting_tech_or_flagrant =
      if (offsetting_tech + offsetting_flagrant > 0) 1 else 0

    val tech_or_flagrant: Int = (if (filtered_clump.collect {
      case defending_team(EventUtils.ParseTechnicalFoul(_)) => ()
      case defending_team(EventUtils.ParseFlagrantFoul(_)) => ()
    }.nonEmpty) 1 else 0) - offsetting_tech_or_flagrant

    val orbs: Int = filtered_clump.collect {
      case attacking_team(EventUtils.ParseOffensiveRebound(_)) => ()
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

    /** Converts score to a number so don't get bitten by number of digits in lexi ordering */
    def score_to_tuple(str: String): (Int, Int) = {
      val regex = "([0-9]+)-([[0-9]+])".r
      str match {
        case regex(s1, s2) => (s1.toInt, s2.toInt) //(ints by construction)
        case _ => (0, 0)
      }
    }

    // (explained in the block comment above)
    val recent_dead_ft_misses = List(prev.evs, clump.evs).flatten.collect {
      case ev @ attacking_team(EventUtils.ParseFreeThrowMade(_)) => ev
      case ev @ attacking_team(EventUtils.ParseFreeThrowMissed(_)) => ev
    }.sortBy(ev => score_to_tuple(ev.score_str)).reverse match { // reverse => highest first
      case fts => fts.drop(1).collect {
        case ev @ attacking_team(EventUtils.ParseFreeThrowMissed(_)) => ev
      }.size
    }

    // this can be wrong if the _prev_ clump had a technical/flagrant, we'll live with that
    val real_deadball_orbs =
      if ((recent_dead_ft_misses == 0) && (tech_or_flagrant == 0)) clump.evs.collect {
        case ev @ attacking_team(EventUtils.ParseOffensiveDeadballRebound(_))
//TODO: is is deliberate that this only covers new format rebounds?
          if !ev.info.startsWith("00:00") //(see spurious deadball rebounds at the end of the period)
        => ()
      }.size else 0

    val turnovers = filtered_clump.collect {
      case attacking_team(EventUtils.ParseTurnover(_)) => ()
    }.size

    PossCalcFragment(
      shots_made_or_missed,
      orbs, real_deadball_orbs,
      ft_event, and_one,
      tech_or_flagrant, offsetting_tech_or_flagrant,
      turnovers
    )
  }

  /** Returns team and opponent possessions based on the lineup data */
  def calculate_possessions(
    lineup_events: Seq[LineupEvent]
  ): List[LineupEvent] = {

    var game_events = lineup_events.iterator.flatMap { lineup =>
      lineup.raw_game_events.iterator.map { ev =>
        ConcurrentClump(List(ev))
      } ++ List(
        ConcurrentClump(Nil, List(lineup))
      ).iterator
    }
    calculate_possessions_by_event(game_events.toStream)
  }

  /** If there are multiple lineups, assign the possessions correctly
      Some examples of complex cases:

      This one gets handled via lineup_balancer
      08:36:00		24-26	Serrel Smith Jr., foul personal
      08:36:00	Jon Elmore, foulon	24-26
      08:36:00	Jon Elmore, rebound offensive	24-26
      08:36:00	Jannson Williams, rebound offensive	24-26
      08:36:00	Rondale Watson, substitution out	24-26
      08:36:00	Taevion Kinsey, substitution in	24-26
      08:36:00	Jon Elmore, 2pt jumpshot 2ndchance missed	24-26
      08:36:00		24-26	Anthony Cowan, substitution in
      08:36:00		24-26	Serrel Smith Jr., substitution out
      08:36:00		24-28	Darryl Morsell, steal
      08:36:00		24-28	Eric Ayala, 2pt layup missed
      08:36:00	Jon Elmore, turnover badpass	24-28
      08:36:00		24-28	Bruno Fernando, 2pt layup pointsinthepaint made
      08:36:00	Jarrod West, rebound defensive	24-28
      08:36:00	Taevion Kinsey, 3pt jumpshot missed	24-28
      08:36:00		24-28	Eric Ayala, rebound defensive

      This one gets handled via lineup_fixer - it's problem is quite fundamental -
      what _is_ the poss<->lineup mapping for: (FG, missed and-1), lineup-change, FG

      01:52:00		30-32	Jordan Murphy, 2pt jumpshot made
      01:52:00	Jalen Smith, foul personal shooting;1freethrow	30-32
      01:52:00		30-32	Jordan Murphy, foulon
      01:52:00		30-32	Amir Coffey, rebound offensive <- NOT ACTUALLY SURE HOW _WHAT_ I SHOULD DO HERE
                         (CAN SCORE MULTIPLE POINTS ON THE SAME POSSESSION WITH DIFFERENT LINEUPS)
      01:52:00		30-32	Jordan Murphy, freethrow 1of1 missed
      subs:
      01:52:00	Darryl Morsell, substitution out	30-32
      01:52:00	Ivan Bender, substitution out	30-32
      01:52:00	Bruno Fernando, substitution in	30-32
      01:52:00	Eric Ayala, substitution in	30-32

  */
  protected def assign_to_right_lineup(
    state: PossState,
    team_stats: PossCalcFragment,
    opponent_stats: PossCalcFragment,
    clump: ConcurrentClump, prev_clump: ConcurrentClump
  ): List[LineupEvent] = {

    val selector_team = modify(_: LineupEvent)(_.team_stats)
    val selector_oppo = modify(_: LineupEvent)(_.opponent_stats)

    /** Where multiple lineups are candidates for possessions, balances them out */
    def lineup_balancer(l: List[LineupEvent]): List[LineupEvent] = l match {
      case head :: Nil => //(just do the really simple case explicitly)
        (head
          .modify(_.team_stats.num_possessions).using(_ + team_stats.total_poss)
          .modify(_.opponent_stats.num_possessions).using(_ + opponent_stats.total_poss)
        ) :: Nil
      case l =>
        val indexed_lineups = l.zipWithIndex

        val balancer_by_dir = List(Direction.Team, Direction.Opponent).map { dir =>
          val indexed_off_evs = l.zipWithIndex.map { case (lineup, index) =>
            val prev = if (index == 0) prev_clump else ConcurrentClump(Nil)
              //(not actually sure how to interpret prev when we _know_ everything's concurrent with lineup breaks!)
            val min_of_interest = clump.min.getOrElse(-1.0)
            val events_of_interest = ConcurrentClump(lineup.raw_game_events.filter(_.min == min_of_interest))
            val approx_stats =  calculate_stats(events_of_interest, prev, dir)
            (-approx_stats.total_poss, index)
          }.sortBy(_._1)

          case class State(tracker: List[(Int, Int)], balancer: Map[Int, Int])
          val starting_state = State(indexed_off_evs, Map.empty.withDefaultValue(0))
          val possessions_available =
            if (dir == Direction.Team) team_stats.total_poss else opponent_stats.total_poss

          val State(_, balancer) = (1 to possessions_available).foldLeft(starting_state) { (state, poss) =>
            val (_, lineup_to_add) = state.tracker.head //(exists by construction)
            val new_balancer = //(add the extra possession to the map of elements to apply in the final loop)
              state.balancer + (lineup_to_add -> (1 + state.balancer(lineup_to_add)))
            val new_tracker = //(remove the possession and re-sort)
              state.modify(_.tracker.at(0)).using(p_i => (p_i._1 + 1, p_i._2)).tracker.sortBy(_._1)

            State(new_tracker, new_balancer)
          }
          dir -> balancer
        }.toMap.withDefaultValue(Map.empty)
        indexed_lineups.map { case (lineup, index) =>
          lineup
            .modify(_.team_stats.num_possessions)
              .using(_ + balancer_by_dir(Direction.Team)(index))
            .modify(_.opponent_stats.num_possessions)
              .using(_ + balancer_by_dir(Direction.Opponent)(index))
        }
    }

    /** Debug util */
    def log_lineup_fix(dir: String, stats: LineupEventStats, l: List[LineupEvent]): Unit = {
      println(
        s"""
        ************Possession fix required [$dir]: [${stats.num_possessions}]:
        [${state.team_stats.summary}]
        + [${team_stats.summary}]
        [${state.opponent_stats.summary}]
        + [${opponent_stats.summary}]
        --
        [${clump.evs}]
        [${l.mkString("\n======\n")}]
        """
      )
    }

    /** Final pass to fix lineups with obviously broken possessions */
    def lineup_fixer(l: List[LineupEvent]): List[LineupEvent] = {
      l.map { lineup =>
        val fixed_lineup = List(("T", selector_team), ("O", selector_oppo)).foldLeft(lineup) {
          case (tofix_lineup, (dir, selector)) =>
            selector(tofix_lineup).using {
              case stats if stats.pts > 0 && (stats.num_possessions <= 0) =>
                if (log_lineup_fixes) log_lineup_fix(dir, stats, l)
                stats.copy(num_possessions = 1)
              case stats if stats.num_possessions < 0 => //(pts == 0)
                if (log_lineup_fixes) log_lineup_fix(dir, stats, l)
                stats.copy(num_possessions = 0)
              case other => other
            }
        }
        //TODO: inject notification here
        fixed_lineup
      }
    }
    (clump.lineups match {
      case head :: tail =>
        (head
          .modify(_.team_stats.num_possessions).using(_ + state.team_stats.total_poss)
          .modify(_.opponent_stats.num_possessions).using(_ + state.opponent_stats.total_poss)
        ) :: tail
      case Nil => Nil
    }) match {
      case l => (lineup_balancer _ andThen lineup_fixer _)(l)
    }
  }

  /** Returns team and opponent possessions based on the raw event data */
  protected def calculate_possessions_by_event(
    raw_events_as_clumps: Seq[ConcurrentClump]
  ): List[LineupEvent] =
  {
    var diagnostic_state = PossState.init //(display only)

    StateUtils.foldLeft(
      raw_events_as_clumps, PossState.init,
      classOf[LineupEvent], concurrent_event_handler[PossState]
    ) {
      case StateEvent.Next(ctx, state, clump @ ConcurrentClump(evs, lineups)) =>
        val team_stats = calculate_stats(clump, state.prev_clump, Direction.Team)
        val opponent_stats = calculate_stats(clump, state.prev_clump, Direction.Opponent)

        diagnostic_state = diagnostic_state.copy(  //(display only)
          team_stats = diagnostic_state.team_stats.sum(team_stats),
          opponent_stats = diagnostic_state.opponent_stats.sum(opponent_stats),
        )

        val (new_state, enriched_lineups) = if (lineups.isEmpty) {
          val updated_state = state.copy(
            team_stats = state.team_stats.sum(team_stats),
            opponent_stats = state.opponent_stats.sum(opponent_stats),
            prev_clump = clump
          )
          (updated_state, Nil)
        } else { // assign new lineups and refresh stats
          val updated_lineups = assign_to_right_lineup(
            state, team_stats, opponent_stats, clump, state.prev_clump
          )
          // Reset state ready for next set of lineups
          (PossState.init.copy(prev_clump = clump), updated_lineups)
        }
        ctx.stateChange(new_state, enriched_lineups)

      case StateEvent.Complete(ctx, state) => //(no additional processing when element list complete)
        ctx.noChange

    } match {
      case FoldStateComplete(_, lineups) =>
        /** Debug util */
        def summarize_state(label: String, state: PossState): Unit = {
          println(s"--------$label------------")
          println(s"[${state.team_stats.summary}]")
          println(s"[${state.opponent_stats.summary}]")
          println("-------------------------")
        }
        if (show_end_of_raw_calcs) {
          summarize_state("-END-", diagnostic_state)
        }
        lineups
    }
  } //(end calculate_possessions)
}
object PossessionUtils extends PossessionUtils
