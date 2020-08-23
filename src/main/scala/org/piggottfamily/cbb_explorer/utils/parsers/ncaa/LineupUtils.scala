package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._


import com.github.dwickern.macros.NameOf._

import shapeless.{Generic, Poly1}
import shapeless.HNil
import shapeless.HList.ListCompat._
import shapeless.labelled._
import shapeless.ops.hlist._
import shapeless.record._
import shapeless.ops.record._
import shapeless.syntax.singleton._
import shapeless.HList.ListCompat._

import com.softwaremill.quicklens._

/** Utilities related to building up the lineup stats */
trait LineupUtils {
  import ExtractorUtils._
  import StateUtils.StateTypes._
  import PossessionUtils.Concurrency

  /** TODO build the stats from the game events */
  def enrich_lineup(lineup: LineupEvent): LineupEvent =
  {
    val scored = lineup.score_info.end.scored - lineup.score_info.start.scored
    val allowed = lineup.score_info.end.allowed - lineup.score_info.start.allowed

    add_stats_to_lineups(lineup.copy(
      team_stats = lineup.team_stats.copy(
        pts = scored,
        plus_minus = scored - allowed
      ),
      opponent_stats = lineup.opponent_stats.copy(
        pts = allowed,
        plus_minus = allowed - scored
      )
    ))
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

  /** Useful scriptlet for checking results
  TODO: note doesn't currently work (since I moved the data model to use Optional)
  // Show results
  import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.LineupUtils
  val res = l.groupBy(t => (t.opponent, t.location_type)).mapValues(
    _.foldLeft((LineupEventStats.empty,LineupEventStats.empty))
    { (acc, v) => (LineupUtils.sum(acc._1, v.team_stats),LineupUtils.sum(acc._2, v.opponent_stats)) }
  ).mapValues(to => (to._1.asJson.toString, to._2.asJson.toString))
  */

  /** Takes a unfiltered set of game events
   *  builds all the counting stats
   */
  protected def enrich_stats(
    lineup: LineupEvent,
    event_parser: LineupEvent.RawGameEvent.PossessionEvent,
    player_filter_coder: Option[String => (Boolean, String)] = None
  ): LineupEventStats => LineupEventStats = { case stats: LineupEventStats =>

    val game_events_as_clumps = Concurrency.lineup_as_raw_clumps(lineup).toStream

    case class StatsBuilder(curr_stats: LineupEventStats, prev_clumps: List[Concurrency.ConcurrentClump])

    StateUtils.foldLeft(
      game_events_as_clumps, StatsBuilder(stats, Nil),
      classOf[LineupEvent], Concurrency.concurrent_event_handler[StatsBuilder]
    ) {
      case StateEvent.Next(ctx, StatsBuilder(curr_stats, prev_clumps), clump @ Concurrency.ConcurrentClump(evs, _)) =>
        val updated_stats = enrich_stats_with_clump(evs, event_parser, player_filter_coder, clump, prev_clumps)(curr_stats)
        ctx.stateChange(StatsBuilder(updated_stats, clump :: prev_clumps))

      case StateEvent.Complete(ctx, _) => //(no additional processing when element list complete)
        ctx.noChange

    } match {
      case FoldStateComplete(StatsBuilder(curr_stats, _), _) =>
        curr_stats
    }
  }

  // Useful lens constants:
  val emptyAssist = LineupEventStats.AssistInfo() //(for filling in options)
  val emptyShotClock = LineupEventStats.ShotClockStats()
  val emptyShotClockModify = modify[Option[LineupEventStats.ShotClockStats]](_.atOrElse(emptyShotClock))

  /**
   * Find an assist at the same time as a shot
   * TODO: handle the case where there are multiple shots for a single assist
   */
  private def find_matching_assist(
    evs: List[LineupEvent.RawGameEvent],
    event_parser: LineupEvent.RawGameEvent.PossessionEvent,
  ): Option[String] = {
    evs.collect {
      case event_parser.AttackingTeam(ev_str @ EventUtils.ParseAssist(player)) => player
    }.headOption
  }

  /**
   * Given an assist, finds the FG - the player and the FG type (represented as a lens
   * path to the assist)
   */
  private def find_matching_fg(
    evs: List[LineupEvent.RawGameEvent],
    event_parser: LineupEvent.RawGameEvent.PossessionEvent
  ): Option[(String, PathLazyModify[LineupEventStats, LineupEventStats.AssistInfo])] = {
    evs.collect {
      case event_parser.AttackingTeam(EventUtils.ParseRimMade(player)) =>
        (player, modify[LineupEventStats](_.ast_rim.atOrElse(emptyAssist)))

      case event_parser.AttackingTeam(EventUtils.ParseTwoPointerMade(player)) =>
        (player, modify[LineupEventStats](_.ast_mid.atOrElse(emptyAssist)))

      case event_parser.AttackingTeam(EventUtils.ParseThreePointerMade(player)) =>
        (player, modify[LineupEventStats](_.ast_3p.atOrElse(emptyAssist)))
    }.headOption
  }

  /** Figure out if the last action was part of a "putback scenario" following an ORB */
  def is_putback(
    curr_clump: Concurrency.ConcurrentClump,
    prev_clumps: List[Concurrency.ConcurrentClump],
    event_parser: LineupEvent.RawGameEvent.PossessionEvent
  ): LineupEvent.RawGameEvent => Boolean = { prev_clumps match {
    case Nil =>
      (ev: LineupEvent.RawGameEvent) => {
        //(if a lineup change has occurred can't be a putback)
        false
      }

    case prev_clump :: _ =>

//TODO: check whether new events have a putback indicator

      // First we decide if the last clump involved opponent offense or not

      // Case 1: opponent offense
      // Then in curr clump:
      //    #(Shot | FT/2) > ORBs:
      //       1st shot isn't putback, 2nd+ are
      //       (we'll just use the shot in order it arrives and see how often that doesn't work)
      //    #(Shot | FT/2) == ORBs
      //       All shots are putbacks

      // Case 2: team offense
      // all shots/FTs must be putbacks (if close enough)

      val last_clump_my_offense = prev_clump.evs.iterator.collect {
        case ev @ event_parser.AttackingTeam(EventUtils.ParseFreeThrowAttempt(_)) => ev
        case ev @ event_parser.AttackingTeam(EventUtils.ParseShotMissed(_)) => ev
        case ev @ event_parser.AttackingTeam(EventUtils.ParseOffensiveRebound(_)) => ev
      }.toStream.headOption

      val threshold = 8.0/60; //(8s)

      last_clump_my_offense.flatMap { ev =>
        // 1] Last playnwas my offense, and then:
        val in_threshold = (ev.min - curr_clump.min.getOrElse(0.0)) < threshold
        if (in_threshold) {
          // 1a] all happened within 8s, so everything "now" is a putback
          Some((ev: LineupEvent.RawGameEvent) => {
            true
          })
        } else {
          // 1b] Longer than 8s ago, so the first event _won't_ be, though subsequent events will be,
          // (see 2)
          None
        }
      }.getOrElse { // 2] Last thing that happened was either opponent offense, or my recycled offense
        val (num_orbs, num_shots, shots_fts) = curr_clump.evs.foldLeft((0, 0, 0)) {
          case (acc, event_parser.AttackingTeam(EventUtils.ParseOffensiveRebound(_))) =>
            acc.copy(_1 = acc._1 + 1)
          case (acc, event_parser.AttackingTeam(EventUtils.ParseShotMissed(_))) =>
            acc.copy(_2 = acc._2 + 1)
          case (acc, event_parser.AttackingTeam(EventUtils.ParseShotMade(_))) =>
            acc.copy(_2 = acc._2 + 1)
          case (acc, event_parser.AttackingTeam(EventUtils.ParseFreeThrowAttempt(_))) =>
            acc.copy(_3 = 1) //(this is basically a latch)
        }
        val num_shots_fts = num_shots + shots_fts

        // (The various cases:)
        if (num_shots_fts <= 1) {
          // 2a] At most 1 shot - no putback
          (ev: LineupEvent.RawGameEvent) => {
            false
          }
        } else if (num_orbs == num_shots_fts) {
          // 2b] one ORB per shot - putback
          (ev: LineupEvent.RawGameEvent) => {
            true
          }
        } else {
          // 2c] The first event (which must be a shot or pair of FTs) ISN'T, all the others are
          // (the first event can't be a made shot o a turnover, though it can be madeFT+missFT)
          val non_putback_ev_analysis = curr_clump.evs.collect {
            case ev @ event_parser.AttackingTeam(EventUtils.ParseFreeThrowMissed(_)) => ev
            case ev @ event_parser.AttackingTeam(EventUtils.ParseShotMissed(_)) => ev
          }
          val non_putback_ev_set = non_putback_ev_analysis.headOption.collect {
            case ev @ event_parser.AttackingTeam(EventUtils.ParseShotMissed(_)) =>
              // 2c.1] First shot
              Set(ev.info)

            case ev @ event_parser.AttackingTeam(EventUtils.ParseFreeThrowMissed(_)) =>
              val ev_info = ev.info
              // 2.c.2] Take all FTs up until the first miss
              curr_clump.evs.iterator.collect {
                case pre_ev @ event_parser.AttackingTeam(EventUtils.ParseFreeThrowAttempt(_)) => pre_ev.info
              }.takeWhile(_ != ev_info).toSet + ev_info
          }.getOrElse(Set())

          (ev: LineupEvent.RawGameEvent) => {
            !non_putback_ev_set(ev.info)
          }
        }
      }
  } }

  /** Check for intentional fouling to prolong the game */
  def is_end_of_game_fouling(
    curr_clump: Concurrency.ConcurrentClump,
    event_parser: LineupEvent.RawGameEvent.PossessionEvent
  ): Boolean = {
    def near_end_of_game(min: Double) = //(go as far as 5OT!)
        (min > 38 && min <= 40) || (min > 43 && min <= 45) || (min > 48 && min <= 50) ||
        (min > 53 && min <= 55) || (min > 58 && min <= 60) || (min > 63 && min <= 65)

    def scores_close_but_behind(ev: LineupEvent.RawGameEvent) = {
      // Attacking team must be ahead by <=10
      val (s1, s2) = score_to_tuple(ev.score_str)
      val diff = if (event_parser.dir == LineupEvent.RawGameEvent.Direction.Team) {
        s1 - s2
      } else {
        s2 - s1
      }
      diff <= 10
    }
    curr_clump.evs.iterator.collect {
      case ev @ event_parser.AttackingTeam(EventUtils.ParseFreeThrowAttempt(_))
        if scores_close_but_behind(ev) && near_end_of_game(ev.min)
      =>
    }.hasNext
  }

  /** Figure out if the last action was part of a transition offense */
  def is_transition(
    curr_clump: Concurrency.ConcurrentClump,
    prev_clumps: List[Concurrency.ConcurrentClump],
    event_parser: LineupEvent.RawGameEvent.PossessionEvent
  ): LineupEvent.RawGameEvent => Boolean = prev_clumps match {
    case Nil =>
      (ev: LineupEvent.RawGameEvent) => {
        //(if a lineup change has occurred we don't include it as transition by policy)
        false
      }


//TODO: I think new event formats maybe have transition so probably check that also?

    case _ if is_end_of_game_fouling(curr_clump, event_parser) =>
      (ev: LineupEvent.RawGameEvent) => {
        false
      }

    // If there are multiple concurrent "possession-terminating" attacking events, then
    // 1) must be ORB-separated
    // 2) I think it's fine to tag them all as transition
    // (so in conclusion we won't process the current clump)

    // Timeout complexities
    // 1) normally works fine because the timeout will be the only thing in the previous clump
    // 2) BUT
    // 2a) prev clump could be Timeout/Opp-Shot or Opp-Shot/Timeout ... I think it's fine to treat
    //     both of these as transition, but it's a bit inconsistent because won't treat it as
    //     transition if there was ALSO a lineup change :(
    // 2b) curr clump could be My-Shot/Timeout (_is_ transition) or Timeout/My-Shot (_not_ transition)
    //     it's not ideal, but it's _OK_ to treat both of these as transition (again - lineup change inconsistency)

    case prev_clump :: _ =>
      val shotMade = prev_clump.evs.iterator.collect {
        case ev @ event_parser.DefendingTeam(EventUtils.ParseTurnover(_)) => ev
        case ev @ event_parser.DefendingTeam(EventUtils.ParseShotMade(_)) => ev
        case ev @ event_parser.DefendingTeam(EventUtils.ParseFreeThrowAttempt(_)) =>
          ev
      }.toStream
      val missAndRebound = prev_clump.evs.iterator.collect {
        case ev @ event_parser.AttackingTeam(EventUtils.ParseDefensiveRebound(_)) => ev
      }.toStream
      val candidates = shotMade.headOption.orElse(missAndRebound.headOption)
      val threshold = 10.0/60.0 //10s
      val is_transition_event = candidates.exists(ev => {
        (ev.min - curr_clump.min.getOrElse(0.0)) < threshold
      })

      (ev: LineupEvent.RawGameEvent) => {
        is_transition_event
      }
  }

  /** Takes a unfiltered set of game events
   * (made this private just so it's clear that this is a sub-call for the protected enrich_stats)
   *  builds all the counting stats
   * - includes context needed for some "possessional processing" (clump)
   * TODO: figure out start of possession times and use (will require prev clump as well I think? and prob some other state)
   */
  private def enrich_stats_with_clump(
    evs: List[LineupEvent.RawGameEvent],
    event_parser: LineupEvent.RawGameEvent.PossessionEvent,
    player_filter_coder: Option[String => (Boolean, String)] = None,
    clump: Concurrency.ConcurrentClump, prev_clumps: List[Concurrency.ConcurrentClump]
  ): LineupEventStats => LineupEventStats = { case stats: LineupEventStats =>
      case class StatsBuilder(curr: LineupEventStats)

      val player_filter = player_filter_coder.map { f => (s: String) => f(s)._1 }
      val player_coder = player_filter_coder.map { f => (s: String) => f(s)._2 }

      val is_transition_builder = is_transition(clump, prev_clumps, event_parser)
      val is_putback_builder = is_putback(clump, prev_clumps, event_parser)

      // A bunch of Lensy plumbing to allow us to increment stats anywhere in the large object
      val selector_shotclock_total = modify[LineupEventStats.ShotClockStats](_.total)
      val selector_shotclock_transition = modify[LineupEventStats.ShotClockStats](_.early)
      val selector_shotclock_putback = modify[LineupEventStats.ShotClockStats](_.orb)

      // Default and overridden versions
      implicit val shotclock_selectors = List(selector_shotclock_total)
      val shot_clock_selector_builder = (ev: LineupEvent.RawGameEvent) => {
        //(will use this for shots, FTs, assists, TOs)
        shotclock_selectors ++
          (if (is_transition_builder(ev)) List(selector_shotclock_transition) else List()) ++
          (if (is_putback_builder(ev)) List(selector_shotclock_transition) else List())
      }

      def increment_misc_count(
        shotclock_selectors: List[PathLazyModify[LineupEventStats.ShotClockStats, Int]]
      ): LineupEventStats.ShotClockStats => LineupEventStats.ShotClockStats =
      { case stat =>
        shotclock_selectors.foldLeft(stat) { (acc, shotclock_selector) =>
          (shotclock_selector).using(_ + 1)(acc)
        }
      }
      // (using implicits here is a slightly dirty hack because of how repetitive the calls
      //  were by the time I realized I needed shot clock selector to be a function of even)
      def increment_misc_stat(
        selector: PathLazyModify[StatsBuilder, LineupEventStats.ShotClockStats]
      )(implicit
        shotclock_selectors: List[PathLazyModify[LineupEventStats.ShotClockStats, Int]]
      ): StatsBuilder => StatsBuilder =
      { case state =>
        selector.using(increment_misc_count(shotclock_selectors))(state)
      }
      def increment_misc_opt_stat(
        selector: PathLazyModify[StatsBuilder, Option[LineupEventStats.ShotClockStats]]
      )(implicit
        shotclock_selectors: List[PathLazyModify[LineupEventStats.ShotClockStats, Int]]
      ): StatsBuilder => StatsBuilder = {
        increment_misc_stat(selector andThenModify emptyShotClockModify)(shotclock_selectors)
      }
      def increment_player_assist(
        player_code: String
      )(implicit
        shotclock_selectors: List[PathLazyModify[LineupEventStats.ShotClockStats, Int]]
      ): List[LineupEventStats.AssistEvent] => List[LineupEventStats.AssistEvent] = {
        case list =>
          (if (list.exists(_.player_code == player_code)) {
            list
          } else {
            LineupEventStats.AssistEvent(player_code) :: list
          }).map {
            case assist_event if assist_event.player_code == player_code =>
              assist_event.modify(_.count).using(increment_misc_count(shotclock_selectors))
            case assist_event => assist_event
          }
      }
      def assist_network_builder(
        player_name: String,
        count_selector: PathLazyModify[StatsBuilder, LineupEventStats.ShotClockStats],
        assist_selector: PathLazyModify[StatsBuilder, Option[List[LineupEventStats.AssistEvent]]]
      )(implicit
        shotclock_selectors: List[PathLazyModify[LineupEventStats.ShotClockStats, Int]]
      ): StatsBuilder => StatsBuilder = { case state =>
        val emptyListModify = modify[Option[List[LineupEventStats.AssistEvent]]](_.atOrElse(Nil))
        val transforms = (increment_misc_stat(count_selector)(shotclock_selectors) :: Nil) ++  //(always increment counts)
          //(increment player info only for team players):
          (player_coder.map(_(player_name)) match {
            case Some(player_code) if event_parser.dir == LineupEvent.RawGameEvent.Direction.Team =>
              Some((assist_selector andThenModify emptyListModify).using(
                increment_player_assist(player_code)(shotclock_selectors)
              ))
            case _ =>
              None
          }).toList

          transforms.foldLeft(state) { (acc, v) => v(acc) }
      }
      def maybe_increment_assisted_stats(
        count_selector: PathLazyModify[StatsBuilder, LineupEventStats.ShotClockStats],
        assisted_selector: PathLazyModify[StatsBuilder, Option[List[LineupEventStats.AssistEvent]]]
      )(implicit
        shotclock_selectors: List[PathLazyModify[LineupEventStats.ShotClockStats, Int]]
      ): StatsBuilder => StatsBuilder = { case state =>
        find_matching_assist(clump.evs, event_parser).map { player_name =>

          assist_network_builder(player_name, count_selector, assisted_selector)(shotclock_selectors)(state)

        }.getOrElse(state)
      }
      def increment_assisted_fg_stats()(implicit
        shotclock_selectors: List[PathLazyModify[LineupEventStats.ShotClockStats, Int]]
      ): StatsBuilder => StatsBuilder = { case state =>
        find_matching_fg(clump.evs, event_parser).map { case (player_name, assist_path) =>
          val curr_selector = modify[StatsBuilder](_.curr)
          val count_selector = curr_selector andThenModify assist_path andThenModify modify[LineupEventStats.AssistInfo](_.counts)
          val assistor_selector = curr_selector andThenModify assist_path andThenModify modify[LineupEventStats.AssistInfo](_.target)

          assist_network_builder(player_name, count_selector, assistor_selector)(shotclock_selectors)(state)

        }.getOrElse(state)
      }

      // Main business logic:

      val id: StatsBuilder => StatsBuilder = s => s
      val starting_state = StatsBuilder(stats)
      (evs.foldLeft(starting_state) {

        // Free throw stats

        case (state, ev @ event_parser.AttackingTeam(ev_str @ EventUtils.ParseFreeThrowMade(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.ft.attempts))
            andThen increment_misc_stat(modify[StatsBuilder](_.curr.ft.made))
          )(state)

        case (state, ev @ event_parser.AttackingTeam(ev_str @ EventUtils.ParseFreeThrowMissed(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.ft.attempts)) andThen id)(state)

        // Field goal stats (rim first, other 2p shots count as "rim")
        // (plus assist enrichment)

        case (state, ev @ event_parser.AttackingTeam(ev_str @ EventUtils.ParseRimMade(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.fg.attempts))
            andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg.made))
              andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_2p.attempts))
                andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_2p.made))
                  andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_rim.attempts))
                    andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_rim.made))

            andThen maybe_increment_assisted_stats(
              modify[StatsBuilder](_.curr.fg_rim.ast.atOrElse(emptyShotClock)),
              modify[StatsBuilder](_.curr.ast_rim.atOrElse(emptyAssist).source),
            )
          )(state)

        case (state, ev @ event_parser.AttackingTeam(ev_str @ EventUtils.ParseRimMissed(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.fg.attempts))
            andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_2p.attempts))
              andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_rim.attempts))
          )(state)

        case (state, ev @ event_parser.AttackingTeam(ev_str @ EventUtils.ParseTwoPointerMade(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.fg.attempts))
            andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg.made))
              andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_2p.attempts))
                andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_2p.made))
                  andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_mid.attempts))
                    andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_mid.made))

              andThen maybe_increment_assisted_stats(
                modify[StatsBuilder](_.curr.fg_mid.ast.atOrElse(emptyShotClock)),
                modify[StatsBuilder](_.curr.ast_mid.atOrElse(emptyAssist).source)
              )
            )(state)

        case (state, ev @ event_parser.AttackingTeam(ev_str @ EventUtils.ParseTwoPointerMissed(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.fg.attempts))
            andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_2p.attempts))
              andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_mid.attempts))
          )(state)

        case (state, ev @ event_parser.AttackingTeam(ev_str @ EventUtils.ParseThreePointerMade(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.fg.attempts))
            andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg.made))
              andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_3p.attempts))
                andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_3p.made))

              andThen maybe_increment_assisted_stats(
                modify[StatsBuilder](_.curr.fg_3p.ast.atOrElse(emptyShotClock)),
                modify[StatsBuilder](_.curr.ast_3p.atOrElse(emptyAssist).source)
              )
            )(state)

        case (state, ev @ event_parser.AttackingTeam(ev_str @ EventUtils.ParseThreePointerMissed(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.fg.attempts))
            andThen increment_misc_stat(modify[StatsBuilder](_.curr.fg_3p.attempts))
          )(state)

        // Misc stats

        case (state, event_parser.AttackingTeam(ev_str @ EventUtils.ParseOffensiveRebound(player)))
          if player_filter.forall(_(player))
          /** TODO: need to ignore actual deadball rebounds..., for now just discard? */
          /** TODO: what about defensive deadball rebounds in old format? */
            && EventUtils.ParseOffensiveDeadballRebound.unapply(ev_str).isEmpty
        =>
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.orb)) andThen id)(state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseDefensiveRebound(player)))
          if player_filter.forall(_(player))
        =>
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.drb)) andThen id)(state)

        case (state, ev @ event_parser.AttackingTeam(EventUtils.ParseTurnover(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_stat(modify[StatsBuilder](_.curr.to)) andThen id)(state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseStolen(player)))
          if player_filter.forall(_(player))
        =>
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.stl)) andThen id)(state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseShotBlocked(player)))
          if player_filter.forall(_(player))
        =>
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.blk)) andThen id)(state)

        case (state, ev @ event_parser.AttackingTeam(EventUtils.ParseAssist(player)))
          if player_filter.forall(_(player))
        =>
          implicit val extended_shotclock_selector = shot_clock_selector_builder(ev)
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.assist))
            andThen increment_assisted_fg_stats()
          )(state)

        case (state, event_parser.AttackingTeam(EventUtils.ParsePersonalFoul(player)))
          if player_filter.forall(_(player))
        =>
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.foul)) andThen id)(state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseFlagrantFoul(player)))
          if player_filter.forall(_(player))
        =>
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.foul)) andThen id)(state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseTechnicalFoul(player)))
          if player_filter.forall(_(player))
        =>
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.foul)) andThen id)(state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseOffensiveFoul(player)))
          if player_filter.forall(_(player))
        =>
          (increment_misc_opt_stat(modify[StatsBuilder](_.curr.foul)) andThen id)(state)

        case (state, _) => state
      }).curr
    }

  /** Enriches the lineup with play-by-play stats for both team and opposition */
  def add_stats_to_lineups(lineup: LineupEvent): LineupEvent = {
    val team_dir = LineupEvent.RawGameEvent.Direction.Team
    val oppo_dir = LineupEvent.RawGameEvent.Direction.Opponent
    val team_event_filter = LineupEvent.RawGameEvent.PossessionEvent(team_dir)
    val oppo_event_filter = LineupEvent.RawGameEvent.PossessionEvent(oppo_dir)
    lineup.copy(
      team_stats = enrich_stats(lineup, team_event_filter)(lineup.team_stats),
      opponent_stats = enrich_stats(lineup, oppo_event_filter)(lineup.opponent_stats),
    )
  }

  /** Create a list of player-specific stats from each lineup event */
  def create_player_events(lineup_event_maybe_bad: LineupEvent, box_lineup: LineupEvent): List[PlayerEvent] = {
    val tidy_ctx = LineupErrorAnalysisUtils.build_tidy_player_context(box_lineup)
    // Since we can process "bad" lineups we do some tidy up activity first:
    val valid_player_codes = box_lineup.players.map(_.code).toSet
    // Don't generate events for players not in the lineup
    val player_tidier = (player: LineupEvent.PlayerCodeId) => {
      val tidyPlayer = ExtractorUtils.build_player_code(
        LineupErrorAnalysisUtils.tidy_player(player.id.name, tidy_ctx), Some(box_lineup.team.team)
      )
      if (valid_player_codes(tidyPlayer.code)) {
        List(tidyPlayer)
      } else {
        Nil
      }
    }
    val lineup_event = lineup_event_maybe_bad.copy(
      players = lineup_event_maybe_bad.players.flatMap(player_tidier),
      players_in = lineup_event_maybe_bad.players_in.flatMap(player_tidier),
      players_out = lineup_event_maybe_bad.players_out.flatMap(player_tidier)
    )
    // OK now back to the main processing:

    val team_dir = LineupEvent.RawGameEvent.Direction.Team
    val team_event_filter = LineupEvent.RawGameEvent.PossessionEvent(team_dir)
    val gen_lineup_event = shapeless.LabelledGeneric[LineupEvent]
    val gen_player_event = shapeless.LabelledGeneric[PlayerEvent]
    val temp_lineup_event = gen_lineup_event.to(lineup_event)
      //(not ideal in that requires player events all be at the front, but a lot simpler than a generic alternative)

    def base_player_event(player_id: LineupEvent.PlayerCodeId) = gen_player_event.from {
      var f: PlayerEvent = null // (just used to infer type in "nameOf")
      (Symbol(nameOf(f.player)) ->> player_id ::
        Symbol(nameOf(f.player_stats)) ->> LineupEventStats.empty ::
        HNil
      ) ++ temp_lineup_event
    }
    val player_filter = (player_id: LineupEvent.PlayerCodeId) => (player_str: String) => {
      val code = ExtractorUtils.build_player_code(
        LineupErrorAnalysisUtils.tidy_player(player_str, tidy_ctx), Some(lineup_event.team.team)
      ).code
      ((code == player_id.code), code)
    }
    lineup_event.players.map { player =>
      val this_player_filter = player_filter(player)
      val player_event = base_player_event(player)
      val player_raw_game_events = lineup_event.raw_game_events.collect {
        case ev @ team_event_filter.AttackingTeam(EventUtils.ParseAnyPlay(player_str))
          if this_player_filter(player_str)._1 => ev
      }
      val player_stats = enrich_stats(
        lineup_event, team_event_filter, Some(this_player_filter)
      )(player_event.player_stats)
      player_event.copy( // will fill in these 2 fields as we go along
        player_stats = player_stats.copy(
          num_events = player_raw_game_events.size
        ),
        raw_game_events = player_raw_game_events
      )
    } //(note: need to keep empty events so we can calculate possessions and hence usage)
  }

  // Very low level:

  /** Adds two lineup stats objects together  - just used for debug */
  // TODO: now I've moved everything to Options, this needs some rework, but that's pretty low priority
  // (since it's only usede for debug)
  // protected def sum(lhs: LineupEventStats, rhs: LineupEventStats): LineupEventStats = {
  //   trait sum_int extends Poly1 {
  //     implicit def case_int2 = at[(Int, Int)](lr => lr._1 + lr._2)
  //   }
  //   trait sum_shot extends sum_int {
  //     val gen_shot = Generic[LineupEventStats.ShotClockStats]
  //     object sum_int_obj extends sum_int
  //     implicit def case_shot2 =
  //       at[(LineupEventStats.ShotClockStats, LineupEventStats.ShotClockStats)] { lr =>
  //         gen_shot.from((gen_shot.to(lr._1) zip gen_shot.to(lr._2)).map(sum_int_obj))
  //       }
  //   }
  //   trait sum_assist extends sum_shot {
  //     val gen_ast = Generic[LineupEventStats.AssistInfo]
  //     object sum_assist_obj extends sum_shot
  //     implicit def case_assist2 =
  //       at[(LineupEventStats.AssistInfo, LineupEventStats.AssistInfo)] { lr =>
  //         LineupEventStats.AssistInfo(
  //           gen_shot.from((gen_shot.to(lr._1.counts) zip gen_shot.to(lr._2.counts)).map(sum_int_obj)),
  //           lr._1.target ++ lr._2.target,
  //           lr._1.source ++ lr._2.source //TODO (this is just for debug so for now just concat the arrarys)
  //         )
  //       }
  //   }
  //   object sum extends sum_assist {
  //     val gen_fg = Generic[LineupEventStats.FieldGoalStats]
  //     object sum_shot_obj extends sum_assist
  //     implicit def case_field2 =
  //       at[(LineupEventStats.FieldGoalStats, LineupEventStats.FieldGoalStats)] { lr =>
  //         gen_fg.from((gen_fg.to(lr._1) zip gen_fg.to(lr._2)).map(sum_shot_obj))
  //       }
  //   }
  //   val gen_lineup = Generic[LineupEventStats]
  //   gen_lineup.from((gen_lineup.to(lhs) zip gen_lineup.to(rhs)).map(sum))
  // }
}
object LineupUtils extends LineupUtils
