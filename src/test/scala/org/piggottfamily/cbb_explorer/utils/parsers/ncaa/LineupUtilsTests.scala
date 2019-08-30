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

        TestUtils.inside(enrich_lineup(test_lineup_1)) {
          case enriched_lineup =>
            enriched_lineup.team_stats.num_events ==> 1
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
    }
  }
}
