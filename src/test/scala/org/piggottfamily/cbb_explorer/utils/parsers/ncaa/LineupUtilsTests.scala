package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

import com.softwaremill.quicklens._

object LineupUtilsTests extends TestSuite with LineupUtils {
  import ExtractorUtils._
  import PossessionUtilsTests.Events //(handy compilation of game events)
  import PossessionUtils.Concurrency
  import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.EventUtilsTests

  val tests = Tests {

    val base_lineup = LineupEvent(
      date = new DateTime(),
      location_type = Game.LocationType.Home,
      start_min = 0.0,
      end_min = -100.0,
      duration_mins = 0.0,
      score_info = LineupEvent.ScoreInfo(
        Game.Score(1, 1), Game.Score(3, 2), 2, 1
      ),
      team = TeamSeasonId(TeamId("TeamA"), Year(2017)),
      opponent = TeamSeasonId(TeamId("TeamB"), Year(2017)),
      lineup_id = LineupEvent.LineupId.unknown,
      players = Nil,
      players_in = Nil,
      players_out = Nil,
      raw_game_events = Nil,
      team_stats = LineupEventStats.empty,
      opponent_stats = LineupEventStats.empty
    )
    val base_player_event = PlayerEvent(
      player_stats = LineupEventStats.empty,
      player = build_player_code("PlayerA, NameA", None),
      date = new DateTime(),
      location_type = Game.LocationType.Home,
      start_min = 0.0,
      end_min = -100.0,
      duration_mins = 0.0,
      score_info = LineupEvent.ScoreInfo(
        Game.Score(1, 1), Game.Score(3, 2), 2, 1
      ),
      team = TeamSeasonId(TeamId("TeamA"), Year(2017)),
      opponent = TeamSeasonId(TeamId("TeamB"), Year(2017)),
      lineup_id = LineupEvent.LineupId.unknown,
      players = Nil,
      players_in = Nil,
      players_out = Nil,
      raw_game_events = Nil,
      team_stats = LineupEventStats.empty,
      opponent_stats = LineupEventStats.empty
    )

    "LineupUtils" - {
      "enrich_lineup" - {
        val test_events_1 = LineupEvent.RawGameEvent(0.0, Some("19:58:00,0-0,team1.1"), None) :: Nil
        val test_events_2 = LineupEvent.RawGameEvent(0.0, None, Some("19:58:00,0-0,opp1.1")) :: Nil

        val test_lineup_1 = base_lineup.copy(raw_game_events = test_events_1)
        val test_lineup_2 = base_lineup.copy(raw_game_events = test_events_2)

        TestUtils.inside(enrich_lineup(test_lineup_1)) {
          case enriched_lineup =>
            enriched_lineup.team_stats.num_events ==> 0
            enriched_lineup.team_stats.num_possessions ==> 0
            enriched_lineup.team_stats.pts ==> 2
            enriched_lineup.team_stats.plus_minus ==> 1

            enriched_lineup.opponent_stats.num_events ==> 0
            enriched_lineup.opponent_stats.num_possessions ==> 0
            enriched_lineup.opponent_stats.pts ==> 1
            enriched_lineup.opponent_stats.plus_minus ==> -1
        }
      }
      "fix_possible_score_swap_bug" - {
        // Data taken from https://stats.ncaa.org/gaame/box_score/4690813?period_no=1

        val correct_score =
          LineupEvent.ScoreInfo(Game.Score(0, 0), Game.Score(67, 78), 0, 0)

        val correct_box_score = LineupEvent(
          date = new DateTime(),
          location_type = Game.LocationType.Home,
          start_min = 0.0,
          end_min = -100.0,
          duration_mins = 0.0,
          score_info = correct_score,
          team = TeamSeasonId(TeamId("TeamA"), Year(2017)),
          opponent = TeamSeasonId(TeamId("TeamB"), Year(2017)),
          lineup_id = LineupEvent.LineupId.unknown,
          players = Nil,
          players_in = Nil,
          players_out = Nil,
          raw_game_events = Nil,
          team_stats = LineupEventStats.empty,
          opponent_stats = LineupEventStats.empty
        )

        val score1 = LineupEvent.ScoreInfo(
          start = Game.Score(8, 14), end = Game.Score(9, 18),
          start_diff = -6, end_diff = -9
        ) -> LineupEvent.ScoreInfo(
          start = Game.Score(14, 8), end = Game.Score(18, 9),
          start_diff = 6, end_diff = 9
        )

        val team_stats1 = LineupEventStats.empty.copy(
          num_events = 9,
          orb = Some(LineupEventStats.ShotClockStats(0, Some(1), Some(2), Some(3), Some(4))),
          num_possessions = 4,
          pts = 1, plus_minus = -3
        ) -> LineupEventStats.empty.copy(
          num_events = 9,
          orb = Some(LineupEventStats.ShotClockStats(0, Some(1), Some(2), Some(3), Some(4))),
          num_possessions = 4,
          pts = 4, plus_minus = 3
        )

        val opp_stats1 = LineupEventStats.empty.copy(
          num_events = 22,
          orb = Some(LineupEventStats.ShotClockStats(10, Some(11), Some(12), Some(13), Some(14))),
          num_possessions = 3, //(was actually 4 but want to demo this not changing)
          pts = 4, plus_minus = 3
        ) -> LineupEventStats.empty.copy(
          num_events = 22,
          orb = Some(LineupEventStats.ShotClockStats(10, Some(11), Some(12), Some(13), Some(14))),
          num_possessions = 3, //(was actually 4 but want to demo this not changing)
          pts = 1, plus_minus = -3
        )

        val score2 = LineupEvent.ScoreInfo(
          start = Game.Score(77, 67), end = Game.Score(78, 67),
          start_diff = 10, end_diff = 11
        ) -> LineupEvent.ScoreInfo(
          start = Game.Score(67, 77), end = Game.Score(67, 78),
          start_diff = -10, end_diff = -11
        )

        val team_stats2 = LineupEventStats.empty.copy(
          num_events = 1,
          orb = Some(LineupEventStats.ShotClockStats(5, Some(6), Some(7), Some(8), Some(9))),
          num_possessions = 1,
          pts = 1, plus_minus = 1
        ) ->LineupEventStats.empty.copy(
          num_events = 1,
          orb = Some(LineupEventStats.ShotClockStats(5, Some(6), Some(7), Some(8), Some(9))),
          num_possessions = 1,
          pts = 0, plus_minus = -1
        )

        val opp_stats2 = LineupEventStats.empty.copy(
          num_events = 1,
          orb = Some(LineupEventStats.ShotClockStats(15, Some(16), Some(17), Some(18), Some(19))),
          num_possessions = 1,
          pts = 0, plus_minus = -1
        ) -> LineupEventStats.empty.copy(
          num_events = 1,
          orb = Some(LineupEventStats.ShotClockStats(15, Some(16), Some(17), Some(18), Some(19))),
          num_possessions = 1,
          pts = 1, plus_minus = 1
        )

        val swapped_lineup = List(
          correct_box_score.copy(
            score_info = score1._1,
            team_stats = team_stats1._1,
            opponent_stats = opp_stats1._1
          ),
          correct_box_score.copy(
            score_info = score2._1,
            team_stats = team_stats2._1,
            opponent_stats = opp_stats2._1
          )
        )

        TestUtils.inside(fix_possible_score_swap_bug(swapped_lineup, correct_box_score)) {
          case fixed_lineup =>
            fixed_lineup ==> List(
              correct_box_score.copy(
                score_info = score1._2,
                team_stats = team_stats1._2,
                opponent_stats = opp_stats1._2
              ),
              correct_box_score.copy(
                score_info = score2._2,
                team_stats = team_stats2._2,
                opponent_stats = opp_stats2._2
              )
            )
        }

        // Check random score inaccuracies don't cause this, ie we do nothing:
        val different_incorrect_score =
          LineupEvent.ScoreInfo(Game.Score(0, 0), Game.Score(92, 100), 0, 0)
        val incorrect_box_score = correct_box_score.copy(
          score_info = different_incorrect_score
        )

        TestUtils.inside(fix_possible_score_swap_bug(swapped_lineup, incorrect_box_score)) {
          case fixed_lineup if fixed_lineup == swapped_lineup =>
        }
      }
      "enrich_stats" - {
        val team_dir = LineupEvent.RawGameEvent.Direction.Team
        val oppo_dir = LineupEvent.RawGameEvent.Direction.Opponent
        val team_event_filter = LineupEvent.RawGameEvent.PossessionEvent(team_dir)
        val oppo_event_filter = LineupEvent.RawGameEvent.PossessionEvent(oppo_dir)
        val zero_stats = LineupEventStats()
        val test_cases = {
          // Edge cases
          List(
          ) -> List()
          List(
            Events.made_opponent //(ignored, wrong direction)
          ) -> List() ::
          // Shots
          List(
            Events.made_3p_team
          ) -> List(
            modify[LineupEventStats](_.fg_3p.attempts),
            modify[LineupEventStats](_.fg.attempts),
            modify[LineupEventStats](_.fg_3p.made),
            modify[LineupEventStats](_.fg.made)
          ) ::
          List(
            Events.missed_3p_team
          ) -> List(
            modify[LineupEventStats](_.fg_3p.attempts),
            modify[LineupEventStats](_.fg.attempts)
          ) ::
          List(
            Events.made_rim_team
          ) -> List(
            modify[LineupEventStats](_.fg_2p.attempts),
            modify[LineupEventStats](_.fg_rim.attempts),
            modify[LineupEventStats](_.fg.attempts),
            modify[LineupEventStats](_.fg_2p.made),
            modify[LineupEventStats](_.fg_rim.made),
            modify[LineupEventStats](_.fg.made)
          ) ::
          List(
            Events.missed_rim_team
          ) -> List(
            modify[LineupEventStats](_.fg_2p.attempts),
            modify[LineupEventStats](_.fg_rim.attempts),
            modify[LineupEventStats](_.fg.attempts)
          ) ::
          List(
            Events.made_mid_team
          ) -> List(
            modify[LineupEventStats](_.fg_2p.attempts),
            modify[LineupEventStats](_.fg_mid.attempts),
            modify[LineupEventStats](_.fg.attempts),
            modify[LineupEventStats](_.fg_2p.made),
            modify[LineupEventStats](_.fg_mid.made),
            modify[LineupEventStats](_.fg.made)
          ) ::
          List(
            Events.missed_mid_team
          ) -> List(
            modify[LineupEventStats](_.fg_2p.attempts),
            modify[LineupEventStats](_.fg_mid.attempts),
            modify[LineupEventStats](_.fg.attempts)
          ) ::
          // Free throws
          List(
            Events.made_ft_team
          ) -> List(
            modify[LineupEventStats](_.ft.attempts),
            modify[LineupEventStats](_.ft.made)
          ) ::
          List(
            Events.missed_ft_team
          ) -> List(
            modify[LineupEventStats](_.ft.attempts)
          ) ::
          // Misc
          // (rebounds)
          List(
            Events.orb_team
          ) -> List(
            modify[LineupEventStats](_.orb.atOrElse(emptyShotClock))
          ) ::
          List(
            Events.drb_team
          ) -> List(
            modify[LineupEventStats](_.drb.atOrElse(emptyShotClock))
          ) ::
          List(
            Events.deadball_orb_team
          ) -> List(
          ) ::
          List(
            Events.deadball_rb_team //(not ideal because could be a defensive rebound)
          ) -> List(
          ) ::
          // (steals)
          List(
            Events.steal_team
          ) -> List(
            modify[LineupEventStats](_.stl.atOrElse(emptyShotClock))
          ) ::
          // (turnovers)
          List(
            Events.turnover_team
          ) -> List(
            modify[LineupEventStats](_.to)
          ) ::
          // (assists)
          List(
            Events.assist_team
          ) -> List(
            modify[LineupEventStats](_.assist.atOrElse(emptyShotClock))
          ) ::
          // (blocks)
          List(
            Events.block_team
          ) -> List(
            modify[LineupEventStats](_.blk.atOrElse(emptyShotClock))
          ) ::
          // Fouls:
          List(
            Events.foul_team
          ) -> List(
            modify[LineupEventStats](_.foul.atOrElse(emptyShotClock))
          ) ::
          List(
            Events.flagrant_team
          ) -> List(
            modify[LineupEventStats](_.foul.atOrElse(emptyShotClock))
          ) ::
          List(
            Events.tech_team
          ) -> List(
            modify[LineupEventStats](_.foul.atOrElse(emptyShotClock))
          ) ::
          List(
            Events.foul_off_team
          ) -> List(
            modify[LineupEventStats](_.foul.atOrElse(emptyShotClock))
          ) ::
          Nil
        }
        val all_at_once = test_cases.fold(test_cases.head) {
          (acc, v) => (acc._1 ++ v._1, acc._2 ++ v._2)
        } match { // Need to add all the assists
          case (l1, l2) => l1 -> (l2 ++ List(
            modify[LineupEventStats](_.fg_3p.ast.atOrElse(emptyShotClock)),
            modify[LineupEventStats](_.fg_rim.ast.atOrElse(emptyShotClock)),
            modify[LineupEventStats](_.fg_mid.ast.atOrElse(emptyShotClock)),

            modify[LineupEventStats](_.ast_3p.atOrElse(emptyAssist).counts) //(only the 3P'er since they are co-located in time)
          ))
        }
        List(team_event_filter, oppo_event_filter).foreach { filter =>
          val adjusted_test_cases = (test_cases ++ List(all_at_once)).map {
            case tuple if filter == team_event_filter => tuple
            case (in, out) => (in.map(Events.reverse_dir), out)
          }
          adjusted_test_cases.foreach { case (test_case, expected_transforms) =>
            val test_case_lineup = base_lineup.copy(raw_game_events = test_case)
            TestUtils.inside((test_case, enrich_stats(test_case_lineup, filter)(zero_stats))) {
              case (_, stats) =>
                val transformed_stats = expected_transforms.foldLeft(stats) {
                  (acc, v) =>
                    val pre1 = (v andThenModify modify[LineupEventStats.ShotClockStats](_.orb)).setTo(None)(acc)
                    val pre2 = (v andThenModify modify[LineupEventStats.ShotClockStats](_.orb)).setTo(None)(pre1)
                    pre2
                } //(get rid of transition/scramble plays - we'll test them later)

                transformed_stats ==> expected_transforms.foldLeft(zero_stats) {
                  (acc, v) => (v andThenModify modify[LineupEventStats.ShotClockStats](_.total)).using(_ + 1)(acc)
                }
            }
          }
        }
        // Check player filter works:
        val player_filter_test = base_lineup.copy(raw_game_events = List(
          Events.foul_team, Events.tech_team
        ))
        TestUtils.inside(
          enrich_stats(
            player_filter_test, team_event_filter, Some((p: String) => (p == "Bruno Fernando", "not_used"))
          )(zero_stats)
        ) {
          case stats =>
            stats ==> zero_stats.modify(_.foul.atOrElse(emptyShotClock).total).setTo(1)
        }

        // player filter, empty case:
        val player_filter_test_2 = base_lineup.copy(raw_game_events =
          EventUtilsTests.all_test_cases.map(LineupEvent.RawGameEvent.team(_, 0.0))
        )
        TestUtils.inside(
          enrich_stats(
            player_filter_test_2,
            team_event_filter, Some(_ => (false, "not_used"))
          )(zero_stats)
        ) {
          case stats =>
            stats ==> zero_stats
        }
        // See "create_player_events" for test of assist.source generation

        // Test assist.target generation (+mid/rim counts to go along with the 3p above):
        val assist_rim_test = base_lineup.copy(raw_game_events = List(
          Events.assist_team, Events.made_rim_team
        ))
        TestUtils.inside(
          enrich_stats(
            assist_rim_test, team_event_filter, Some((p: String) => (p == "Kyle Guy", s"code($p)"))
          )(zero_stats)
        ) {
          case stats =>
            stats ==> zero_stats
              .modify(_.assist.atOrElse(emptyShotClock).total).setTo(1)
              .modify(_.ast_rim).setTo(Some(LineupEventStats.AssistInfo().copy(
                counts = LineupEventStats.ShotClockStats().copy(total = 1),
                target = Some(LineupEventStats.AssistEvent(
                  "code(Eric Carter)",
                  LineupEventStats.ShotClockStats().copy(total = 1)
                ) :: Nil
              ))))
        }
        val assist_mid_test = base_lineup.copy(raw_game_events = List(
          Events.assist_team, Events.made_mid_team
        ))
        TestUtils.inside(
          enrich_stats(
            assist_mid_test, team_event_filter, Some((p: String) => (p == "Kyle Guy", s"code($p)"))
          )(zero_stats)
        ) {
          case stats =>
            stats ==> zero_stats
              .modify(_.assist.atOrElse(emptyShotClock).total).setTo(1)
              .modify(_.ast_mid).setTo(Some(LineupEventStats.AssistInfo().copy(
                counts = LineupEventStats.ShotClockStats().copy(total = 1),
                target = Some(LineupEventStats.AssistEvent(
                  "code(Eric Ayala)",
                  LineupEventStats.ShotClockStats().copy(total = 1)
                ) :: Nil
              ))))
        }
      }
      "add_stats_to_lineups" - {
        val test_lineup = base_lineup.copy(
          raw_game_events = List(Events.foul_team, Events.turnover_opponent)
        )
        TestUtils.inside(add_stats_to_lineups(test_lineup)) {
          case lineup =>
            lineup ==> test_lineup
              .modify(_.team_stats.foul.atOrElse(emptyShotClock).total).setTo(1)
              .modify(_.opponent_stats.to.total).setTo(1)
        }
      }

      /** Builds 2 clumps of events with a specified time difference */
      def clump_scenario_builder(
        current: List[LineupEvent.RawGameEvent], before: List[LineupEvent.RawGameEvent],
        current_sec: Double, before_sec: Double
      ): (Concurrency.ConcurrentClump, List[Concurrency.ConcurrentClump]) = {
        val sec_to_min = 1.0/60;
        val current_clump = Concurrency.ConcurrentClump(
          current.map(_.copy(min = current_sec*sec_to_min))
        )
        val before_clump = if (before.isEmpty) None else Some(Concurrency.ConcurrentClump(
          before.map(_.copy(min = before_sec*sec_to_min))
        ))
        (current_clump, before_clump.toList ++ Nil)
      }

      "is_scramble" - {
        val team_dir = LineupEvent.RawGameEvent.Direction.Team
        val team_event_filter = LineupEvent.RawGameEvent.PossessionEvent(team_dir)

        // All the different cases described in "is_scramble" method
        // (since 0a, 1b, 2aa, and 1ab all use the internal "get_first_off_ev_set", we'll
        //  try to cover all the cases of that:
        //  - IGNORE made shot with assist: 1b.2
        //  - made shot with +1: 0a.1
        //  - FT attempt (old format, with rebound): 2aa.1
        //  - FT attempt (new format, ignores rebounds): 2ab.2
        //  - missed shots: 1aa.1, 1b.1, 2ab.1
        //  - TOs then missed shot (TO allowed): 1ab.1
        //  - TOs then missed shot (TO _not_ allowed): 0a.2
        //  - 2nd chance event is first event, concurrent with an earlier missed shot: 2aa.2
        //  - dangling FT case: FT miss, (gap), FT made: 1ab.2

        // 0a: (no prev clump, ORBs present, multiple events): 0a.1, 0a.2
        // 1aa: (small gap between off clumps, ORBs present, multiple events): 1aa.1
        // 1ab: (small gap between off clumps, no ORBs present, multiple events): 1ab.1, 1ab.2 (+ special case: 2ab.3)
        // 1b: (large gap between off clumps, ORBs present, multiple events): 1b.1, 1b.2
        // 2aa: (def then off, small/irrelevant gap, ORBs present, multiple events): 2aa.1, 2aa.2
        // 2ab: (small gap between clumps, small/irrelevant gap, no ORBs present): 2ab.1
        // 2ab: (no prev clump, gap irrelevant, ORBs present but only one offensive event): 2ab.2

        // N/A no offensive events in current
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.foul_team :: Events.made_opponent :: Nil,
            Events.made_team :: Events.missed_ft1_team :: Events.orb_team :: Events.missed_rim_team :: Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "N/A"
        }
        // 0a.2: (nothing), small gap, turnover, ORB, made shot
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.turnover_team :: Events.orb_team :: Events.missed_rim_team :: Nil,
            Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "0a"
          current_clump.evs.map(is_scramble_builder) ==> List(true, true, false)
        }
        // 1aa.1: missed, short gap, rebound, missed shot + made shot (no ORBs in next clump needed)
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.missed_rim_team :: Events.made_team :: Events.orb_team :: Nil,
            Events.missed_rim_team :: Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "1aa"
          current_clump.evs.map(is_scramble_builder) ==> List(true, true, true)
        }
        // 1ab.1: made shot, foul, (short gap), FTM, FTm, made shot, FTM [no ORBs]
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.turnover_team :: Events.made_team :: Events.missed_ftp1_team :: Nil,
            Events.missed_team :: Events.orb_team :: Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "1ab"
          current_clump.evs.map(is_scramble_builder) ==> List(true, false, false)
        }
        // 1ab.2: dangling FT bug workaround
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.missed_ft_team :: Events.made_team :: Nil,
            Events.missed_ft_team :: Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "1ab"
          current_clump.evs.map(is_scramble_builder) ==> List(false, false)
        }
        // 1b.1: like 1aa.1 but with large gap
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.missed_rim_team :: Events.made_team :: Events.orb_team :: Nil,
            Events.missed_rim_team :: Nil,
            12.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "1b"
          current_clump.evs.map(is_scramble_builder) ==> List(false, true, true)
        }
        // 1b.2: like 1aa.1 but with large gap and including an assist
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.made_team :: Events.assist_team :: Events.missed_ftp1_team :: Events.orb_team :: Events.missed_rim_team :: Nil,
            Events.missed_rim_team :: Nil,
            12.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "1b"
          current_clump.evs.map(is_scramble_builder) ==> List(false, false, false, true, true)
        }
        // 2aa.1: def, small gap, old format FTs split by ORBs
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.made_ft_team :: Events.orb_team :: Events.made_ft1_team :: Events.made_ft2_team :: Nil,
            Events.foul_team :: Events.made_opponent :: Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "2aa"
          current_clump.evs.map(is_scramble_builder) ==> List(false, true, true, true)
        }
        // 2aa.2: def, long/irrelevant gap, 2ndchange miss, ORB, miss
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.missed_team_2ndchance :: Events.orb_team :: Events.made_team :: Nil,
            Events.foul_team :: Events.made_opponent :: Nil,
            12.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "2aa"
          current_clump.evs.map(is_scramble_builder) ==> List(true, true, false)
        }
        // 2ab.1: defense, small gap, missed shot, deadball rebound, FT, FT (no ORBs)
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.missed_rim_team :: Events.deadball_rb_team :: Events.made_ft_team :: Events.missed_ft2_team :: Nil,
            Events.missed_opponent :: Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "2ab"
          current_clump.evs.map(is_scramble_builder) ==> List(false, false, false, false)
        }
        // 2ab.2: (nothing), small gap, FT1, FT2, ORB, FT3 (ie all one offensive event)
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.made_ft1_team :: Events.orb_team :: Events.made_ft2_team :: Events.made_ft2_team :: Nil,
            Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "2ab"
          current_clump.evs.map(is_scramble_builder) ==> List(false, false, false, false)
        }
        // 2ab.3: Like 1ab.1 but no misses in prev clump, so first event can't be a rebound either
        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.turnover_team :: Events.made_team :: Events.missed_ftp1_team :: Nil,
            Events.made_team :: Nil,
            10.0, 5.0
          )
          val (is_scramble_builder, debug_category) = is_scramble(current_clump, before_clumps, team_event_filter, player_version = false)
          debug_category ==> "2ab"
          current_clump.evs.map(is_scramble_builder) ==> List(false, false, false)
        }

        // Finally we run a sample scenario through "enrich_stats" to prove is_scramble is called
        // (team version and player version)

        {
          val (current_clump, before_clumps) = clump_scenario_builder(
            Events.orb_team :: Events.missed_rim_team :: Events.orb_team :: Events.made_team :: Nil,
            Events.missed_rim_team :: Nil,
            10.0, 5.0
          )
          val test_case_lineup = base_lineup.copy(raw_game_events =
            before_clumps.flatMap(_.evs) ++ current_clump.evs
          )
          val zero_stats = LineupEventStats()

          TestUtils.inside(
            enrich_stats(
              test_case_lineup, team_event_filter, Some((p: String) => (p == "Eric Carter", "not_used"))
            )(zero_stats)
          ) {
            case stats =>
              stats ==> zero_stats
                .modify(_.fg.attempts.total).setTo(2)
                .modify(_.fg_rim.attempts.total).setTo(2)
                .modify(_.fg_2p.attempts.total).setTo(2)
                .modify(_.fg.attempts.orb).setTo(Some(1))
                .modify(_.fg_rim.attempts.orb).setTo(Some(1))
                .modify(_.fg_2p.attempts.orb).setTo(Some(1))
          }
          TestUtils.inside(
            enrich_stats(
              test_case_lineup, team_event_filter, None
            )(zero_stats)
          ) {
            case stats =>
              stats ==> zero_stats
                .modify(_.orb.atOrElse(emptyShotClock).total).setTo(2)
                .modify(_.fg.attempts.total).setTo(3)
                .modify(_.fg.made.total).setTo(1)
                .modify(_.fg_rim.attempts.total).setTo(2)
                .modify(_.fg_2p.attempts.total).setTo(2)
                .modify(_.fg_3p.attempts.total).setTo(1)
                .modify(_.fg_3p.made.total).setTo(1)
                .modify(_.fg.attempts.orb).setTo(Some(2))
                .modify(_.fg.made.orb).setTo(Some(1))
                .modify(_.fg_rim.attempts.orb).setTo(Some(1))
                .modify(_.fg_2p.attempts.orb).setTo(Some(1))
                .modify(_.fg_3p.attempts.orb).setTo(Some(1))
                .modify(_.fg_3p.made.orb).setTo(Some(1))
          }
        }
      }
      /** Handy util to sub scores in for end of game situations */
      val sub_score = (new_score: String) => (ev: LineupEvent.RawGameEvent) => {
        val sub_score_str = (s: String) => {
          s.split(",").toList match {
            case min :: score :: rest => (min :: new_score :: rest).mkString(",")
            case _ => s
          }
        }
        ev.copy(
          team = ev.team.map(sub_score_str),
          opponent = ev.opponent.map(sub_score_str),
          min = ev.min
        )
      }
      "is_end_of_game_fouling_vs_fastbreak" - {
        val team_dir = LineupEvent.RawGameEvent.Direction.Team
        val oppo_dir = LineupEvent.RawGameEvent.Direction.Opponent
        val team_event_filter = LineupEvent.RawGameEvent.PossessionEvent(team_dir)
        val oppo_event_filter = LineupEvent.RawGameEvent.PossessionEvent(oppo_dir)
        List(
          // Basic logic:
          (team_event_filter, 37.5, "60-58", Events.made_team :: Nil, false),
          (team_event_filter, 38.1, "60-58", Events.made_ft_team :: Nil, true),
          (team_event_filter, 38.1, "60-58", Events.made_team :: Nil, false),
          (team_event_filter, 38.1, "60-58", Events.made_ft_opponent :: Nil, false),
          (team_event_filter, 38.1, "58-60", Events.made_ft_team :: Nil, false),
          (team_event_filter, 38.1, "70-58", Events.made_team :: Nil, false),
          // Check account for the score increase on made FTs:
          (team_event_filter, 38.1, "60-59", Events.made_ft_team :: Nil, false),
          (team_event_filter, 38.1, "60-59", Events.missed_ft_team :: Nil, true),

          // Opponents:
          (oppo_event_filter, 38.1, "60-58", Events.made_ft_opponent :: Nil, false),
          (oppo_event_filter, 38.1, "58-60", Events.made_ft_opponent :: Nil, true),

          //OTs:
          (team_event_filter, 42.0, "60-58", Events.made_ft_team :: Nil, false),
          (team_event_filter, 43.2, "60-58", Events.made_ft_team :: Nil, true),
          (team_event_filter, 45.1, "60-58", Events.made_ft_team :: Nil, false),
          (team_event_filter, 49.5, "60-58", Events.made_ft_team :: Nil, true),
          (team_event_filter, 51.1, "60-58", Events.made_ft_team :: Nil, false),
          (team_event_filter, 55.0, "60-58", Events.made_ft_team :: Nil, true),
          (team_event_filter, 57.1, "60-58", Events.made_ft_team :: Nil, false),
          (team_event_filter, 59.0, "60-58", Events.made_ft_team :: Nil, true),
          (team_event_filter, 63.0, "60-58", Events.made_ft_team :: Nil, false),
          (team_event_filter, 64.9, "60-58", Events.made_ft_team :: Nil, true),

        ).foreach {
          case (filter, min, score, pre_evs, result) =>
            val evs = pre_evs.map(sub_score(score)).map(_.copy(min = min))
            TestUtils.inside(evs) {
              case _ =>
                is_end_of_game_fouling_vs_fastbreak(Concurrency.ConcurrentClump(evs), filter) ==> result
            }
        }
      }

      "is_transition" - {
        val team_dir = LineupEvent.RawGameEvent.Direction.Team
        val oppo_dir = LineupEvent.RawGameEvent.Direction.Opponent
        val team_event_filter = LineupEvent.RawGameEvent.PossessionEvent(team_dir)
        val oppo_event_filter = LineupEvent.RawGameEvent.PossessionEvent(oppo_dir)

        /** (will make all clumps close in score and then use mins to keep them apart) */
        val sub_score_clump = (clump: Concurrency.ConcurrentClump) => {
          clump.copy(evs = clump.evs.map(sub_score("60-58")))
        }

        // (for completeness, check empty case that is optimized out)
        val (_, debug_category) = is_transition(
          Concurrency.ConcurrentClump(Nil), Nil, team_event_filter, player_version = false
        )
        debug_category ==> "N/A"

        // 0a: check end of game fouling
        // 1a.*: result depends on gap
        List((9.0, true), (11.0, false)).foreach {  case (gap, should_be_transition) =>
          // 1a.a.1: opponent miss + team DRB
          {
            val (current_clump, before_clumps) = clump_scenario_builder(
              Events.made_team :: Nil,
              Events.missed_opponent :: Events.drb_team :: Nil,
              39*60 + gap, 39*60 //(won't be 0a because no FTs involved)
            )
            val (is_transition_builder, debug_category) = is_transition(
              sub_score_clump(current_clump), before_clumps, team_event_filter, player_version = false
            )
            debug_category ==> (if (should_be_transition) "1a.a" else "NOT")
            current_clump.evs.map(ev => is_transition_builder(ev, false)) ==> current_clump.evs.map(_ => should_be_transition)
            current_clump.evs.map(ev => is_transition_builder(ev, true)) ==> current_clump.evs.map(_ => false)
          }
          // 1a.a.2: opponent make
          //TODO
          // 1a.b.1: opponent miss, next clump has DRB
          //TODO

          // 0a: check end of game fouling
          {
            val (current_clump, before_clumps) = clump_scenario_builder(
              Events.made_ft_team :: Nil,
              Events.missed_opponent :: Events.drb_team :: Nil,
              39*60 + gap, 39*60
            )
            val (is_transition_builder, debug_category) = is_transition(
              sub_score_clump(current_clump), before_clumps, team_event_filter, player_version = false
            )
            debug_category ==> "0a"
            current_clump.evs.map(ev => is_transition_builder(ev, false)) ==> current_clump.evs.map(_ => false)
          }
        }
        // 1b.*: result does _not_ depend on gap
        List((11.0, true), (9.0, true)).foreach {  case (gap, should_be_transition) =>
          // 1b.a.1: nothing then fast break
          //TODO
          // 1b.b.1: dangling FT scenario
          //TODO
          // 1b.X.2: my offense then fast break - should be excluded
          //TODO
        }

        //TODO: run enrich_stats and check transition/scrable cases are handled correctly
        // (see is_scramble test code)

        //TODO: once done don't forget to turn debug off
      }

      //TODO: commenting this out since it needs rework following move to optionals
      // but this is low prio because it's just a debug function
      /**
      "sum" - {
        val test1 = LineupEventStats.empty
          .modify(_.num_possessions).setTo(1)
          .modify(_.fg.made.total).setTo(2)
          .modify(_.orb.total).setTo(3)
        val test2 = LineupEventStats.empty
          .modify(_.fg.made.total).setTo(3)
          .modify(_.drb.total).setTo(4)
        sum(test1, test2) ==> LineupEventStats.empty
          .modify(_.num_possessions).setTo(1)
          .modify(_.fg.made.total).setTo(5)
          .modify(_.orb.total).setTo(3)
          .modify(_.drb.total).setTo(4)
      }
      */
      "create_player_events" - {
        val test_lineup = base_lineup.copy( //TODO: set possessions
          players = {
            "Smith, Jalen" ::
            "Morsell, Darryl" ::
            "Layman, Jake" ::
            Nil
          }.map(build_player_code(_, None)),
          raw_game_events = {
            Events.made_team :: // Jalen Smith made 3
            Events.made_opponent :: // Jalen Smith made 3, ignore
            Events.drb_team :: // Morsell DRB
            Events.drb_opponent :: // Morsell DRB, ignore
            Events.deadball_rb_team :: //Ignore)
            Events.deadball_rb_opponent :://(Double ignore!)
            Events.assist_team :: //(Ignore, wrong player)
            Events.assist_opponent :://(Double ignore!)
            Events.block_team :: //Layman block
            Events.block_opponent :: //Layman block, ignore
            Events.steal_opponent :: //(Double ignore!)
            Events.made_team :: // Another Jalen Smith made 3!
            Events.missed_team :: // This event comes from a different player not in the box score
            Nil
          },
          team_stats = base_lineup.team_stats.modify(_.num_possessions).setTo(7)
        )
        val in_lineup = test_lineup.copy( // Corrupt the box score to ensure it gets tidied up
            players = build_player_code("JALEN SMITH", None) :: //check gets reformatted by the box score
              build_player_code("Ayala, Eric", None) :: //(will remove from box lineup so his events get ignored)
              test_lineup.players.filterNot(_.code == "JaSmith")
        )
        val test_box = test_lineup.copy(
          team_stats = base_lineup.team_stats,
          opponent_stats = base_lineup.opponent_stats,
        )

        TestUtils.inside(create_player_events(in_lineup, test_box)) {
          case player1 :: player2 :: player3 :: Nil =>
            player1 ==> base_player_event.copy(
              date = player1.date, //(has been out by 1ms!)
              player = test_lineup.players(0),
              player_stats = LineupEventStats.empty
                .modify(_.num_events).setTo(2)
                .modify(_.fg_3p.attempts.total).setTo(2)
                .modify(_.fg_3p.made.total).setTo(2)
                .modify(_.fg_3p.ast.atOrElse(emptyShotClock).total).setTo(2)
                .modify(_.fg.attempts.total).setTo(2)
                .modify(_.fg.made.total).setTo(2)
                .modify(_.fg.made.total).setTo(2)
                .modify(_.ast_3p).setTo(Some(LineupEventStats.AssistInfo().copy(
                  source = Some(LineupEventStats.AssistEvent(
                    "KyGuy",
                    LineupEventStats.ShotClockStats().copy(total = 2)
                  ) :: Nil)
                )))
              ,
              players = test_lineup.players,
              raw_game_events = Events.made_team :: Events.made_team :: Nil,
              team_stats = test_lineup.team_stats,
              opponent_stats = test_lineup.opponent_stats
            )
            player2 ==> base_player_event.copy(
              date = player2.date, //(has been out by 1ms!)
              player = test_lineup.players(1),
              player_stats = LineupEventStats.empty
                .modify(_.num_events).setTo(1)
                .modify(_.drb.atOrElse(emptyShotClock).total).setTo(1),
              players = test_lineup.players,
              raw_game_events = Events.drb_team :: Nil,
              team_stats = test_lineup.team_stats,
              opponent_stats = test_lineup.opponent_stats
            )
            player3 ==> base_player_event.copy(
              date = player3.date, //(has been out by 1ms!)
              player = test_lineup.players(2),
              player_stats = LineupEventStats.empty
                .modify(_.num_events).setTo(1)
                .modify(_.blk.atOrElse(emptyShotClock).total).setTo(1),
              players = test_lineup.players,
              raw_game_events = Events.block_team :: Nil,
              team_stats = test_lineup.team_stats,
              opponent_stats = test_lineup.opponent_stats
            )
        }
      }
    }
  }
}
