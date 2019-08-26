package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object LineupUtilsTests extends TestSuite with LineupUtils {

  val tests = Tests {
    "LineupUtils" - {
      "enrich_lineup" - {
        val test_events_1 = LineupEvent.RawGameEvent(Some("19:58:00,0-0,team1.1"), None, Some(1), None) :: Nil
        val test_events_2 = LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,opp1.1"), None, Some(1)) :: Nil

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

        val test_lineup_1 = base_lineup.copy(raw_game_events = test_events_1)
        val test_lineup_2 = base_lineup.copy(raw_game_events = test_events_2)

        TestUtils.inside(enrich_lineup(test_lineup_1, None)) {
          case (enriched_lineup, None) =>
            enriched_lineup.team_stats.num_events ==> 1
            enriched_lineup.team_stats.num_possessions ==> 1
            enriched_lineup.team_stats.pts ==> 2
            enriched_lineup.team_stats.plus_minus ==> 1

            enriched_lineup.opponent_stats.num_events ==> 0
            enriched_lineup.opponent_stats.num_possessions ==> 0
            enriched_lineup.opponent_stats.pts ==> 1
            enriched_lineup.opponent_stats.plus_minus ==> -1
        }
        TestUtils.inside(enrich_lineup(test_lineup_1, Some(test_lineup_2))) {
          case (enriched_lineup, Some(`test_lineup_2`)) =>
            //(we're just validating that we correctly leave the prev lineup alone)
        }

        TestUtils.inside(enrich_lineup(test_lineup_1, Some(test_lineup_1))) {
          case (enriched_lineup, Some(adjusted_lineup)) =>
            enriched_lineup.team_stats.num_possessions ==> 1
            enriched_lineup.opponent_stats.num_possessions ==> 0

            // The important bit:
            adjusted_lineup.team_stats.num_possessions ==> -1
            adjusted_lineup.opponent_stats.num_possessions ==> 0
        }
        TestUtils.inside(enrich_lineup(test_lineup_2, Some(test_lineup_1))) {
          case (enriched_lineup, Some(`test_lineup_1`)) =>
          enriched_lineup.team_stats.num_events ==> 0
          enriched_lineup.team_stats.num_possessions ==> 0
          enriched_lineup.opponent_stats.num_events ==> 1
          enriched_lineup.opponent_stats.num_possessions ==> 1
        }
        TestUtils.inside(enrich_lineup(test_lineup_2, Some(test_lineup_2))) {
          case (enriched_lineup, Some(adjusted_lineup)) =>
            enriched_lineup.team_stats.num_possessions ==> 0
            enriched_lineup.opponent_stats.num_possessions ==> 1

            // The important bit:
            adjusted_lineup.team_stats.num_possessions ==> 0
            adjusted_lineup.opponent_stats.num_possessions ==> -1
        }
      }
      "calculate_possessions" - {
        val test_events_1 = //(set expected possession -1)
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,player, jumpball lost"), None, None, None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:01,0-0,player, jumpball won"), None, None) ::
          LineupEvent.RawGameEvent(Some("19:58:02,0-0,team1.1"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:03,0-0,player, substitution out"), Some(0), None) ::
          LineupEvent.RawGameEvent(Some("19:58:04,0-0,team1.2"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:05,0-0,opp1.1"), None, Some(0)) ::
          LineupEvent.RawGameEvent(None, Some("19:58:06,0-0,player, substitution in"), None, Some(0)) ::
          LineupEvent.RawGameEvent(Some("19:58:07,0-0, team2.1"), None, Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:08,0-0,PLAYER Leaves Game"), Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:09,0-0,PLAYER Enters Game"), Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:10,0-0,opp2.1"), None, Some(1)) ::
          LineupEvent.RawGameEvent(Some("19:58:11,0-0,team3.1"), None, Some(2), None) ::
          Nil
        TestUtils.inside(calculate_possessions(test_events_1, None)) {
          case (3, 2, events, false) =>
            events ==> test_events_1.map(e => e.copy(
              team_possession = e.team_possession.map(_ + 1),
              opponent_possession = e.opponent_possession.map(_ + 1),
            ))
        }

        val test_events_2 = LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,opp1.1")) :: Nil
        TestUtils.inside(calculate_possessions(test_events_2, None)) {
          case (0, 1, events, false) =>
            events ==> events.map(_.copy(opponent_possession = Some(1)))
        }

        val jumpball_event = Some("19:58:01,0-0,player, jumpball lost")
        val timeout_event = Some("19:58:02,0-0,player, team, timeout short")
        val block_event = Some("19:58:03,0-0,player Blocked Shot")
        val steal_event = Some("19:58:04,0-0,player Steal")
        val personal_foul_event = Some("19:58:05,0-0,player, foul personal; info")
        val technical_foul_event = Some("19:58:06,0-0,player, foul technical classa; info")
        val foul_info_event = Some("19:58:07,0-0,player, foulon")
        val test_events_3 = //(set expected possession -1)
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,player, team1.1"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, jumpball_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, timeout_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, block_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, steal_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, personal_foul_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, technical_foul_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, foul_info_event, Some(0), None) ::
          LineupEvent.RawGameEvent(Some("19:58:10,0-0,player, team1.2"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:20,0-0,player, opp1.1"), None, Some(0)) ::
          LineupEvent.RawGameEvent(jumpball_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(timeout_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(block_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(steal_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(personal_foul_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(technical_foul_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(foul_info_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(None, Some("19:58:30,0-0,player, opp1.2"), None, Some(0)) ::
          Nil
        TestUtils.inside(calculate_possessions(test_events_3, None)) {
          case (1, 1, events, false) =>
            events ==> test_events_3.map(e => e.copy(
              team_possession = e.team_possession.map(_ + 1),
              opponent_possession = e.opponent_possession.map(_ + 1),
            ))
        }

        // Check adjustments to previous lineup:
        val misc_team_event =
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,player, team1.2"), None, Some(0), None)
        val misc_opponent_event =
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,player, team1.2"), None, None, Some(0))

        TestUtils.inside(calculate_possessions(test_events_1, Some(misc_team_event))) {
          case (_, _, _, true) =>
        }
        TestUtils.inside(calculate_possessions(test_events_1, Some(misc_opponent_event))) {
          case (_, _, _, false) =>
        }
        TestUtils.inside(calculate_possessions(test_events_2, Some(misc_team_event))) {
          case (_, _, _, false) =>
        }
        TestUtils.inside(calculate_possessions(test_events_2, Some(misc_opponent_event))) {
          case (_, _, _, true) =>
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
    }
  }
}
