package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object PossessionUtilsTests extends TestSuite with PossessionUtils {
  import ExtractorUtils._
  import StateUtils.StateTypes._
  //(these used to live in here but moved them centrally)
  import org.piggottfamily.cbb_explorer.models.ncaa.LineupEvent.RawGameEvent.Direction
  import org.piggottfamily.cbb_explorer.models.ncaa.LineupEvent.RawGameEvent.PossessionEvent

  /** A handy compilation of events */
  object Events {
    val jump_won_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "19:58:00,0-0,Bruno Fernando, jumpball won")
    val jump_won_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = jump_won_team.team.get)
    val jump_lost_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = "19:58:00,0-0,Kavell Bigby-Williams, jumpball lost")

    val turnover_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "08:44:00,20-23,Bruno Fernando, turnover badpass")
    val turnover_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = turnover_team.team.get)
    val steal_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "05:10,55-68,MASON III,FRANK Steal")
    val steal_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = steal_team.team.get)

    val made_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,SMITH,JALEN made Three Point Jumper")
    val made_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = made_team.team.get)
    val missed_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "02:28:00,27-38,Eric Ayala, 3pt jumpshot 2ndchance missed")
    val missed_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = missed_team.team.get)
    val made_ft_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,DREAD,MYLES made Free Throw")
    val made_ft_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = made_ft_team.team.get)
    val made_ft1_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "05:10,55-68,Kevin Anderson, freethrow 1of2 made")
    val made_ft1_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = made_ft1_team.team.get)
    val made_ft2_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "05:10,55-68,Kevin Anderson, freethrow 2of2 made")
    val made_ft2_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = made_ft2_team.team.get)
    val missed_ft_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,DREAD,MYLES missed Free Throw")
    val missed_ft_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = missed_ft_team.team.get)
    val missed_ft1_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "05:10,55-68,Kevin Anderson, freethrow 1of2 missed")
    val missed_ft1_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = missed_ft1_team.team.get)
    val missed_ft2_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "05:10,55-68,Kevin Anderson, freethrow 2of2 missed")
    val missed_ft2_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = missed_ft2_team.team.get)
    // other shots:
    val made_3p_team = made_team
    val missed_3p_team = missed_team
    val made_rim_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,Eric Carter, 2pt layup made")
    val made_mid_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,Eric Ayala, 2pt jumpshot made")
    val missed_rim_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,Eric Carter, 2pt layup missed")
    val missed_mid_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,Eric Ayala, 2pt jumpshot missed")

    val orb_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,Darryl Morsell, rebound offensive")
    val orb_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = orb_team.team.get)
    val drb_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,Darryl Morsell, rebound defensive")
    val drb_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = drb_team.team.get)
    val deadball_rb_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,TEAM Deadball Rebound")
    val deadball_rb_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = deadball_rb_team.team.get)
    val deadball_orb_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "04:28:0,52-59,Team, rebound offensivedeadball")
    val deadball_orb_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = deadball_orb_team.team.get)
    // other events
    val assist_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,Kyle Guy, assist")
    val block_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,04:53,55-69,LAYMAN,JAKE Blocked Shot")

    val foul_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "10:00,51-60,MYKHAILIUK,SVI Commits Foul")
    val foul_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = foul_team.team.get)
    val tech_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "06:43:00,55-79,Bruno Fernando, foul technical classa;2freethrow")
    val tech_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = tech_team.team.get)
    val flagrant_team = LineupEvent.RawGameEvent.team(min = 0.0, s = "03:42:00,55-79,Eric Carter, foul personal flagrant1;2freethrow")
    val flagrant_opponent = LineupEvent.RawGameEvent.opponent(min = 0.0, s = flagrant_team.team.get)

    def reverse_dir: LineupEvent.RawGameEvent => LineupEvent.RawGameEvent = {
      case LineupEvent.RawGameEvent(a, Some(b), None) => LineupEvent.RawGameEvent(a, None, Some(b))
      case LineupEvent.RawGameEvent(a, None, Some(b)) => LineupEvent.RawGameEvent(a, Some(b), None)
      case ev => ev
    }
  }

  val tests = Tests {
    "PossessionUtils" - {

      val base_lineup = LineupEvent(
        date = new DateTime(),
        location_type = Game.LocationType.Home,
        start_min = 0.0,
        end_min = 1.0,
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

      "concurrent_event_handler" - {
        // (test check_for_concurrent_event and rearrange_concurrent_event)

        val test_events @ (
          ev1 :: ev2 :: ev3 :: ev4 :: ev5 :: ev6 :: ev7 :: ev8
          :: Nil
        ) =
          // Test minutes based clumping
          LineupEvent.RawGameEvent.team(min = 0.4, s = "20:00,ev-1") ::
          LineupEvent.RawGameEvent.opponent(min = 0.5, s = "19:00,ev-2") ::
          LineupEvent.RawGameEvent.team(min = 0.9, s = "18:00,ev-3") ::
          LineupEvent.RawGameEvent.opponent(min = 0.9, s = "17:00,ev-4") ::
          LineupEvent.RawGameEvent.team(min = 0.8, s = "16:00,ev-5") ::
          LineupEvent.RawGameEvent.opponent(min = 0.8, s = "15:00,ev-6") ::
          //(game break!)
          LineupEvent.RawGameEvent.opponent(min = 0.8, s = "20:00,ev-7") ::
          LineupEvent.RawGameEvent.team(min = 1.0, s = "19:00,ev-8") ::
          // Test possession directing based clumping
          Nil
        val test_events_in = test_events.map(ev => ConcurrentClump(ev :: Nil, Nil))


        TestUtils.inside(
          StateUtils.foldLeft(test_events_in, PossState.init, concurrent_event_handler[PossState]) {
            case StateEvent.Next(ctx, state, ConcurrentClump(evs, _)) =>
              ctx.stateChange(state, ConcurrentClump(evs))
            case StateEvent.Complete(ctx, _) =>
              ctx.noChange
          }
        ) {
          case FoldStateComplete(_,
            ConcurrentClump(`ev1` :: Nil, _) ::
            ConcurrentClump(`ev2` :: Nil, _) ::
            ConcurrentClump(`ev3` :: `ev4` :: Nil, _) ::
            ConcurrentClump(`ev5` :: `ev6` :: Nil, _) ::
            ConcurrentClump(`ev7` :: Nil, _) ::
            ConcurrentClump(`ev8` :: Nil, _) ::
            Nil
          ) =>
        }

        // Check with lineups:
        val lineup1 = base_lineup.copy(start_min = 1.0, end_min = 2.0)
        val lineup2 = base_lineup.copy(start_min = 2.0, end_min = 3.0)

        val test_lineups =
          (ev2 -> Nil) ::
          (ev3 -> List(base_lineup)) ::
          (ev4 -> List(lineup1)) ::
          (ev5 -> List(lineup2)) ::
          Nil
        val test_lineups_in = test_lineups.map { case (ev, l) => ConcurrentClump(ev :: Nil, l) }

        TestUtils.inside(
          StateUtils.foldLeft(test_lineups_in, PossState.init, concurrent_event_handler[PossState]) {
            case StateEvent.Next(ctx, state, clump) =>
              ctx.stateChange(state, clump)
            case StateEvent.Complete(ctx, _) =>
              ctx.noChange
          }
        ) {
          case FoldStateComplete(_,
            ConcurrentClump(`ev2` :: Nil, Nil) ::
            ConcurrentClump(`ev3` :: `ev4` :: Nil, `base_lineup` :: `lineup1` :: Nil) ::
            ConcurrentClump(`ev5` :: Nil, `lineup2` :: Nil) ::
            Nil
          ) =>
        }
      }

      "PossCalcFragment" - {
        val frag1 = PossCalcFragment(1, 2, 3, 4, 5, 6, 7, 8)
        val frag2 = PossCalcFragment(1, 3, 5, 7, 9, 11, 13, 15)
        val frag_sum = PossCalcFragment(2, 5, 8, 11, 14, 17, 20, 23)

        frag1.sum(frag2) ==> frag_sum
        frag1.total_poss ==> 2
        frag1.summary ==> "total=[2] = shots=[1] - (orbs=[2] + db_orbs=[3]) + (ft_sets=[4] - techs=[6]) + to=[8] { +1s=[5] offset_techs=[7] }"
      }

      "calculate_stats" - {

        val empty_fragment = PossCalcFragment()
        // Tests:
        val tests =
          // Shots made/missed
          List(Events.made_team, Events.missed_team, Events.made_team) ->
            empty_fragment.copy(shots_made_or_missed = 3) ::
          List(Events.missed_team) ->
            empty_fragment.copy(shots_made_or_missed = 1) ::
          List(Events.made_opponent, Events.missed_opponent) ->
            empty_fragment ::
          // Free throws
          List(Events.made_ft1_team, Events.missed_ft1_team) ->
            empty_fragment.copy(ft_events = 1) ::
          List(Events.made_ft_team, Events.missed_ft_team) -> //(legacy format)
            empty_fragment.copy(ft_events = 1) ::
          List(Events.made_ft2_team) -> //(only FT1s count)
            empty_fragment ::
          List(Events.made_ft1_team, Events.made_ft1_team, Events.made_ft1_team, Events.made_ft1_team) ->
            empty_fragment.copy(ft_events = 1) :: //(at most one FT set per clump)
          List(Events.made_ft_team, Events.made_ft_team, Events.made_ft_team, Events.made_ft_team) ->
            empty_fragment.copy(ft_events = 1) :: //(at most one FT set per clump)
          List(Events.made_ft1_opponent, Events.missed_ft_opponent) ->
            empty_fragment ::
          // Free throws that definitely _aren't_ and-1s:
          List(Events.missed_ft1_team) -> //(not all single FTs are and-1s)
            empty_fragment.copy(ft_events = 1) ::
          List(Events.missed_ft_team) -> //(not all single FTs are and-1s; legacy)
            empty_fragment.copy(ft_events = 1) ::
          List(Events.made_ft1_team) -> //(not all single FTs are and-1s; legacy)
            empty_fragment.copy(ft_events = 1) ::
          List(Events.made_ft_team) -> //(not all single FTs are and-1s; legacy)
            empty_fragment.copy(ft_events = 1) ::
          List(Events.made_ft1_team, Events.missed_ft1_team, Events.made_team) -> //(2 FTs so ignore make)
            empty_fragment.copy(shots_made_or_missed = 1, ft_events = 1) ::
          List(Events.made_ft_team, Events.missed_ft_team, Events.made_team) -> //(2 FTs so ignore make; legacy)
            empty_fragment.copy(shots_made_or_missed = 1, ft_events = 1) ::
          List(Events.made_ft1_team, Events.missed_team) -> //(missed so ignore 1 FT; legacy)
            empty_fragment.copy(shots_made_or_missed = 1, ft_events = 1) ::
          List(Events.made_ft_team, Events.missed_team) -> //(missed so ignore 1 FT; legacy)
            empty_fragment.copy(shots_made_or_missed = 1, ft_events = 1) ::
          // and-1s (see also above/below-under-prev-testing)
          List(Events.made_ft1_team, Events.made_team) ->
            empty_fragment.copy(shots_made_or_missed = 1, ignored_and_ones = 1) ::
          List(Events.made_ft_team, Events.made_team) -> //(legacy)
            empty_fragment.copy(shots_made_or_missed = 1, ignored_and_ones = 1) ::
          List(Events.missed_ft1_team, Events.made_team) ->
            empty_fragment.copy(shots_made_or_missed = 1, ignored_and_ones = 1) ::
          List(Events.missed_ft_team, Events.made_team) -> //(legacy)
            empty_fragment.copy(shots_made_or_missed = 1, ignored_and_ones = 1) ::
          List(Events.missed_ft1_opponent, Events.made_opponent) ->
            empty_fragment ::
          List(Events.missed_ft_opponent, Events.made_opponent) -> //(legacy)
            empty_fragment ::
          // offsetting techs/flagrants
          List(Events.tech_team, Events.tech_opponent) ->
            empty_fragment.copy(offsetting_bad_fouls = 1) ::
          List(Events.flagrant_team, Events.flagrant_opponent) ->
            empty_fragment.copy(offsetting_bad_fouls = 1) ::
          List(Events.tech_team, Events.tech_opponent, Events.tech_team, Events.tech_opponent) ->
            empty_fragment.copy(offsetting_bad_fouls = 1) :: //(counts at most for one)
          List(Events.tech_team, Events.tech_opponent, Events.flagrant_team, Events.flagrant_opponent) ->
            empty_fragment.copy(offsetting_bad_fouls = 1) :: //(counts at most for one)
          List(Events.flagrant_team, Events.flagrant_opponent, Events.flagrant_team, Events.flagrant_opponent) ->
            empty_fragment.copy(offsetting_bad_fouls = 1) :: //(counts at most for one)
          // non-offsetting techs/flagrants
          List(Events.flagrant_team, Events.tech_opponent) ->
            empty_fragment.copy(bad_fouls = 1) ::
          List(Events.tech_team, Events.flagrant_opponent) ->
            empty_fragment.copy(bad_fouls = 1) ::
          List(Events.flagrant_opponent) ->
            empty_fragment.copy(bad_fouls = 1) ::
          List(Events.tech_opponent, Events.tech_opponent) ->
            empty_fragment.copy(bad_fouls = 1) :: //(counts at most for one)
          List(Events.flagrant_opponent, Events.flagrant_opponent) ->
            empty_fragment.copy(bad_fouls = 1) :: //(counts at most for one)
          List(Events.tech_opponent) ->
            empty_fragment.copy(bad_fouls = 1) ::
          List(Events.tech_opponent, Events.tech_opponent, Events.tech_opponent) ->
            empty_fragment.copy(bad_fouls = 1) :: //(counts at most for one)
          // ORBs
          List(Events.orb_team, Events.orb_team, Events.orb_team) ->
            empty_fragment.copy(liveball_orbs = 3) ::
          List(Events.orb_opponent) ->
            empty_fragment ::
          // deadball ORBs
          List(Events.deadball_rb_team, Events.deadball_rb_team) -> //currently only handle new format rebounds
            empty_fragment ::
          List(Events.deadball_orb_team, Events.deadball_orb_team) ->
            empty_fragment.copy(actual_deadball_orbs = 2) ::
          List(
            Events.deadball_orb_team, Events.deadball_orb_team,
            Events.missed_ft1_team, Events.missed_ft2_team, Events.made_ft2_team
          ) ->
            empty_fragment.copy(ft_events = 1) ::
          List(
            Events.deadball_orb_team, Events.deadball_orb_team, // mismatch vs number of misses IGNORED
            Events.missed_ft1_team, Events.made_ft2_team, Events.missed_ft2_team
          ) ->
            empty_fragment.copy(ft_events = 1) ::
          List(
            Events.deadball_orb_team, Events.deadball_orb_team, // only miss was the last free throw
            Events.made_ft1_team, Events.made_ft2_team, Events.missed_ft2_team.copy(
              team = Events.missed_ft2_team.team.map(_.replace(
                Events.missed_ft2_team.score_str, "100-100"))
            )
          ) ->
            empty_fragment.copy(ft_events = 1, actual_deadball_orbs = 2) ::
          List(Events.deadball_orb_team.copy( //ignore end of period deadball rebounds
            team = Events.deadball_orb_team.team.map(_.replace(
              Events.deadball_orb_team.date_str, "00:00:10"
            ))
          )) ->
            empty_fragment ::
          List(Events.deadball_orb_opponent) ->
            empty_fragment ::
          // turnovers
          List(Events.turnover_team, Events.turnover_team) ->
            empty_fragment.copy(turnovers = 2) ::
          List(Events.turnover_opponent) ->
            empty_fragment ::
          // misc other events
          List(
            Events.jump_won_team, Events.jump_won_opponent, Events.jump_lost_opponent,
            Events.steal_team, Events.steal_opponent, Events.drb_team, Events.drb_opponent,
            Events.foul_team, Events.foul_opponent
          ) ->
            empty_fragment ::
          Nil

        // Both directions and checking prev is ignored
        tests.map { case (l, out) =>
          (l, l.map(Events.reverse_dir), out)
        }.foreach { case (test_in, test_in_oppo, expected_out) =>
          TestUtils.inside(
            (test_in,
              calculate_stats(ConcurrentClump(test_in), ConcurrentClump(Nil), Direction.Team),
              calculate_stats(ConcurrentClump(test_in_oppo), ConcurrentClump(Nil), Direction.Opponent),
              calculate_stats(ConcurrentClump(Nil), ConcurrentClump(test_in), Direction.Team),
              calculate_stats(ConcurrentClump(Nil), ConcurrentClump(test_in_oppo), Direction.Opponent)
            )
          ) {
            case (_, `expected_out`, `expected_out`, `empty_fragment`, `empty_fragment`) =>
          }
        }
        val test_prevs =
          // Check use of prev in determining and-1s
          (List(Events.made_team), List(Events.missed_ft1_team),
            empty_fragment.copy(ignored_and_ones = 1)) ::
          (List(Events.made_team), List(Events.made_ft1_team),
            empty_fragment.copy(ignored_and_ones = 1)) ::
          (List(Events.missed_team), List(Events.made_ft1_team),
            empty_fragment.copy(ft_events = 1)) ::
          (List(Events.made_team, Events.made_opponent), List(Events.made_ft1_team),
            empty_fragment.copy(ft_events = 1)) ::
          // Check use of prev in determining deadball rebounds
          (List(Events.missed_ft1_team), List(
            Events.deadball_orb_team, Events.deadball_orb_team, // mismatch vs number of misses IGNORED
            Events.made_ft2_team, Events.missed_ft2_team
          ),
            empty_fragment) ::
          (List(Events.made_ft1_team), List(
            Events.deadball_orb_team, Events.deadball_orb_team, // only miss was the last free throw
            Events.made_ft2_team, Events.missed_ft2_team.copy(
              team = Events.missed_ft2_team.team.map(_.replace(
                Events.missed_ft2_team.score_str, "100-100"))
            )
          ),
            empty_fragment.copy(actual_deadball_orbs = 2)) ::
          Nil

          test_prevs.map { case (prev, curr, out) =>
            (prev, prev.map(Events.reverse_dir), curr, curr.map(Events.reverse_dir), out)
          }.foreach { case (prev_in, prev_in_oppo, curr_in, curr_in_oppo, expected_out) =>
            TestUtils.inside(
              (prev_in, curr_in,
                calculate_stats(ConcurrentClump(curr_in), ConcurrentClump(prev_in), Direction.Team),
                calculate_stats(ConcurrentClump(curr_in_oppo), ConcurrentClump(prev_in_oppo), Direction.Opponent)
              )
            ) {
              case (_, _, `expected_out`, `expected_out`) =>
            }
          }
      }

      val test_lineups = { // Pair of lineups that test assign_to_right_lineup and top-level fn
        val lineup_2a = base_lineup.copy(
          team_stats = base_lineup.team_stats.copy(
            pts = 0,
            num_possessions = 0
          ),
          opponent_stats = base_lineup.opponent_stats.copy(
            pts = 0,
            num_possessions = 0
          ),
          raw_game_events = (List( //3:1 ratio
            Events.made_opponent, Events.made_opponent, Events.made_opponent
          ).map(_.copy(min = 2.0)) ++
            List(
              Events.made_opponent).map(_.copy(min = 1.0) //ignored because time is wrong
            )).sortBy(_.min)
        )
        val lineup_2b = base_lineup.copy(
          team_stats = base_lineup.team_stats.copy(
            pts = 0,
            num_possessions = 0
          ),
          opponent_stats = base_lineup.opponent_stats.copy(
            pts = 0,
            num_possessions = 0
          ),
          raw_game_events = (List( //3:1 ratio
            Events.made_opponent, Events.made_opponent,
          ).map(_.copy(min = 3.0)) ++  //ignored because time is wrong
            List(
              Events.made_opponent).map(_.copy(min = 2.0)
            )).sortBy(_.min)
        )
        lineup_2a :: lineup_2b :: Nil
      }

      "assign_to_right_lineup" - {

        // Check first that in a single lineup case we apply the stats
        // and that we fix the score>0, poss==0 case
        val lineup1 = base_lineup.copy(
          team_stats = base_lineup.team_stats.copy(
            pts = 0,
            num_possessions = 0
          ),
          opponent_stats = base_lineup.opponent_stats.copy(
            pts = 2,
            num_possessions = 0
          )
        )
        val state1 = PossState.init.copy(
          team_stats = PossCalcFragment(shots_made_or_missed = 2)
        )
        val team_stats1 = PossCalcFragment(shots_made_or_missed = 1)
        val oppo_stats1 = PossCalcFragment()
        val clump1 = ConcurrentClump(Nil, List(lineup1))
        TestUtils.inside(assign_to_right_lineup(
          state1, team_stats1, oppo_stats1, clump1, ConcurrentClump(Nil)
        )) {
          case lineup :: Nil =>
            lineup.team_stats.num_possessions ==> 3
            lineup.opponent_stats.num_possessions ==> 1
        }

        // Now we'll check the multi-lineup case
        // where we have to balance the possessions
        // across 2 lineups

        val state2 = PossState.init.copy(
          team_stats = PossCalcFragment(shots_made_or_missed = 2),
          opponent_stats = PossCalcFragment(shots_made_or_missed = 3),
        )
        val team_stats2 = PossCalcFragment(shots_made_or_missed = 0)
        val oppo_stats2 = PossCalcFragment(shots_made_or_missed = 4)
        val clump2 = ConcurrentClump(
          List(Events.made_team.copy(min = 2.0)), // (event itself is ignored but the time is used)
          test_lineups
        )
        TestUtils.inside(assign_to_right_lineup(
          state2, team_stats2, oppo_stats2, clump2, ConcurrentClump(Nil)
        )) {
          case lineup_a :: lineup_b :: Nil =>
            lineup_a.team_stats.num_possessions ==> 2 //(from state2)
            lineup_a.opponent_stats.num_possessions ==> 6 //(3 from state2 and then 75% of the fragment)
            lineup_b.team_stats.num_possessions ==> 0 //(allowed because score==0)
            lineup_b.opponent_stats.num_possessions ==> 1 //(25% of the fragment)
        }
      }

      "calculate_possessions" - {
        // We've already tested the logic, just need to confirm that
        TestUtils.inside(calculate_possessions(test_lineups)) {
          case lineup_a :: lineup_b :: Nil =>
            lineup_a.team_stats.num_possessions ==> 0
            lineup_a.opponent_stats.num_possessions ==> 5 // (all the min1 and all-1 min2 events)
            lineup_b.team_stats.num_possessions ==> 0
            lineup_b.opponent_stats.num_possessions ==> 2 //(1 min2 event, and all the min 3 events)
        }
      }
    }
  }
  /** (Useful debug if assertion below fails) */
  private def print_useful_list_diff(l1: List[_], l2: List[_]): Unit = {
    println("+++++++++++++++++++")
    println(
      l1.zip(l2).map { t2 =>
        val are_eq = if (t2._1 == t2._2) "T" else "F"
        s"[$are_eq]: [${t2._1}] V [${t2._2}]"
      }.mkString("\n")
    )
    println("-------------------")
  }
}
