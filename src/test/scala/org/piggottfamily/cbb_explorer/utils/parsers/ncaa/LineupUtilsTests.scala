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
          orb = LineupEventStats.ShotClockStats(0, 1, 2, 3, 4),
          num_possessions = 4,
          pts = 1, plus_minus = -3
        ) -> LineupEventStats.empty.copy(
          num_events = 9,
          orb = LineupEventStats.ShotClockStats(0, 1, 2, 3, 4),
          num_possessions = 4,
          pts = 4, plus_minus = 3
        )

        val opp_stats1 = LineupEventStats.empty.copy(
          num_events = 22,
          orb = LineupEventStats.ShotClockStats(10, 11, 12, 13, 14),
          num_possessions = 3, //(was actually 4 but want to demo this not changing)
          pts = 4, plus_minus = 3
        ) -> LineupEventStats.empty.copy(
          num_events = 22,
          orb = LineupEventStats.ShotClockStats(10, 11, 12, 13, 14),
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
          orb = LineupEventStats.ShotClockStats(5, 6, 7, 8, 9),
          num_possessions = 1,
          pts = 1, plus_minus = 1
        ) ->LineupEventStats.empty.copy(
          num_events = 1,
          orb = LineupEventStats.ShotClockStats(5, 6, 7, 8, 9),
          num_possessions = 1,
          pts = 0, plus_minus = -1
        )

        val opp_stats2 = LineupEventStats.empty.copy(
          num_events = 1,
          orb = LineupEventStats.ShotClockStats(15, 16, 17, 18, 19),
          num_possessions = 1,
          pts = 0, plus_minus = -1
        ) -> LineupEventStats.empty.copy(
          num_events = 1,
          orb = LineupEventStats.ShotClockStats(15, 16, 17, 18, 19),
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
            modify[LineupEventStats](_.fg_3p.attempts.total),
            modify[LineupEventStats](_.fg.attempts.total),
            modify[LineupEventStats](_.fg_3p.made.total),
            modify[LineupEventStats](_.fg.made.total)
          ) ::
          List(
            Events.missed_3p_team
          ) -> List(
            modify[LineupEventStats](_.fg_3p.attempts.total),
            modify[LineupEventStats](_.fg.attempts.total)
          ) ::
          List(
            Events.made_rim_team
          ) -> List(
            modify[LineupEventStats](_.fg_2p.attempts.total),
            modify[LineupEventStats](_.fg_rim.attempts.total),
            modify[LineupEventStats](_.fg.attempts.total),
            modify[LineupEventStats](_.fg_2p.made.total),
            modify[LineupEventStats](_.fg_rim.made.total),
            modify[LineupEventStats](_.fg.made.total)
          ) ::
          List(
            Events.missed_rim_team
          ) -> List(
            modify[LineupEventStats](_.fg_2p.attempts.total),
            modify[LineupEventStats](_.fg_rim.attempts.total),
            modify[LineupEventStats](_.fg.attempts.total)
          ) ::
          List(
            Events.made_mid_team
          ) -> List(
            modify[LineupEventStats](_.fg_2p.attempts.total),
            modify[LineupEventStats](_.fg_mid.attempts.total),
            modify[LineupEventStats](_.fg.attempts.total),
            modify[LineupEventStats](_.fg_2p.made.total),
            modify[LineupEventStats](_.fg_mid.made.total),
            modify[LineupEventStats](_.fg.made.total)
          ) ::
          List(
            Events.missed_mid_team
          ) -> List(
            modify[LineupEventStats](_.fg_2p.attempts.total),
            modify[LineupEventStats](_.fg_mid.attempts.total),
            modify[LineupEventStats](_.fg.attempts.total)
          ) ::
          // Free throws
          List(
            Events.made_ft_team
          ) -> List(
            modify[LineupEventStats](_.ft.attempts.total),
            modify[LineupEventStats](_.ft.made.total)
          ) ::
          List(
            Events.missed_ft_team
          ) -> List(
            modify[LineupEventStats](_.ft.attempts.total)
          ) ::
          // Misc
          // (rebounds)
          List(
            Events.orb_team
          ) -> List(
            modify[LineupEventStats](_.orb.total)
          ) ::
          List(
            Events.drb_team
          ) -> List(
            modify[LineupEventStats](_.drb.total)
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
            modify[LineupEventStats](_.stl.total)
          ) ::
          // (turnovers)
          List(
            Events.turnover_team
          ) -> List(
            modify[LineupEventStats](_.to.total)
          ) ::
          // (assists)
          List(
            Events.assist_team
          ) -> List(
            modify[LineupEventStats](_.assist.total)
          ) ::
          // (blocks)
          List(
            Events.block_team
          ) -> List(
            modify[LineupEventStats](_.blk.total)
          ) ::
          // Fouls:
          List(
            Events.foul_team
          ) -> List(
            modify[LineupEventStats](_.foul.total)
          ) ::
          List(
            Events.flagrant_team
          ) -> List(
            modify[LineupEventStats](_.foul.total)
          ) ::
          List(
            Events.tech_team
          ) -> List(
            modify[LineupEventStats](_.foul.total)
          ) ::
          List(
            Events.foul_off_team
          ) -> List(
            modify[LineupEventStats](_.foul.total)
          ) ::
          Nil
        }
        val all_at_once = test_cases.fold(test_cases.head) {
          (acc, v) => (acc._1 ++ v._1, acc._2 ++ v._2)
        } match { // Need to add all the assists
          case (l1, l2) => l1 -> (l2 ++ List(
            modify[LineupEventStats](_.fg_3p.ast.total),
            modify[LineupEventStats](_.fg_rim.ast.total),
            modify[LineupEventStats](_.fg_mid.ast.total),
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
                stats ==> expected_transforms.foldLeft(zero_stats) {
                  (acc, v) => v.using(_ + 1)(acc)
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
            player_filter_test, team_event_filter, Some(_ == "Bruno Fernando")
          )(zero_stats)
        ) {
          case stats =>
            stats ==> zero_stats.modify(_.foul.total).setTo(1)
        }

        // player filter, empty case:
        val player_filter_test_2 = base_lineup.copy(raw_game_events =
          EventUtilsTests.all_test_cases.map(LineupEvent.RawGameEvent.team(_, 0.0))
        )
        TestUtils.inside(
          enrich_stats(
            player_filter_test_2,
            team_event_filter, Some(_ => false)
          )(zero_stats)
        ) {
          case stats =>
            stats ==> zero_stats
        }
        // See "create_player_events" for test of assist.source generation
      }
      "add_stats_to_lineups" - {
        val test_lineup = base_lineup.copy(
          raw_game_events = List(Events.foul_team, Events.turnover_opponent)
        )
        TestUtils.inside(add_stats_to_lineups(test_lineup)) {
          case lineup =>
            lineup ==> test_lineup
              .modify(_.team_stats.foul.total).setTo(1)
              .modify(_.opponent_stats.to.total).setTo(1)
        }
      }
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
                .modify(_.fg_3p.ast.total).setTo(2)
                .modify(_.fg.attempts.total).setTo(2)
                .modify(_.fg.made.total).setTo(2)
                .modify(_.fg.made.total).setTo(2)
                .modify(_.ast_3p).setTo(LineupEventStats.AssistInfo().copy(
                  source = LineupEventStats.AssistEvent(
                    "Kyle Guy",
                    LineupEventStats.ShotClockStats().copy(total = 2)
                  ) :: Nil
                ))
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
                .modify(_.drb.total).setTo(1),
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
                .modify(_.blk.total).setTo(1),
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
