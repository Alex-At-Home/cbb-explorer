package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object LineupErrorAnalysisUtilsTests extends TestSuite {
  import LineupErrorAnalysisUtils._

  val tests = Tests {
    "LineupErrorAnalysisUtils" - {

      //TODO

      "tidy_player" - {
        //TODO: adequately test by last test of ExtractorUtils.build_partial_lineup_list
      }

      "validate_lineup" - {
        val now = new DateTime()
        val all_players @ (player1 :: player2 :: player3 :: player4 :: player5 ::
          player6 :: player7 :: Nil) = List(
            "Player One", "Player Two", "Player Three",
            "Player Four", "Player Five", "Player Six", "Player Seven"
          ).map(ExtractorUtils.build_player_code(_, None))
        val all_player_set = all_players.map(_.code).toSet
        val player8 = ExtractorUtils.build_player_code("Player Eight", None)

        val valid_players = player1 :: player2 :: player3 :: player4 :: player5 :: Nil
        val too_few_players = player1 :: player2 :: player3 :: player4 :: Nil
        val unknown_player = player1 :: player2 :: player3 :: player4 :: player8 :: Nil
        val multi_bad = player8 :: valid_players

        val my_team = TeamSeasonId(TeamId("TestTeam1"), Year(2017))
        val other_team = TeamSeasonId(TeamId("TestTeam2"), Year(2017))
        val base_lineup = LineupEvent(
          date = now,
          location_type = Game.LocationType.Home,
          start_min = 0.0,
          end_min = -100.0,
          duration_mins = 0.0,
          score_info = LineupEvent.ScoreInfo.empty,
          team = my_team,
          opponent = other_team,
          lineup_id = LineupEvent.LineupId.unknown,
          players = Nil,
          players_in = Nil,
          players_out = Nil,
          raw_game_events = Nil,
          team_stats = LineupEventStats.empty,
          opponent_stats = LineupEventStats.empty
        )

        val good_lineup = base_lineup.copy(players = valid_players)
        val lineup_too_many = base_lineup.copy(players = all_players)
        val lineup_too_few = base_lineup.copy(players = too_few_players)
        val lineup_unknown_player = base_lineup.copy(players = unknown_player)
        val lineup_multi_bad = base_lineup.copy(players = multi_bad)

        TestUtils.inside(validate_lineup(good_lineup, all_player_set).toList) {
          case List() =>
        }
        TestUtils.inside(validate_lineup(lineup_too_many, all_player_set).toList) {
          case List(ValidationError.WrongNumberOfPlayers) =>
        }
        TestUtils.inside(validate_lineup(lineup_too_few, all_player_set).toList) {
          case List(ValidationError.WrongNumberOfPlayers) =>
        }
        TestUtils.inside(validate_lineup(lineup_unknown_player, all_player_set).toList) {
          case List(ValidationError.UnknownPlayers) =>
        }
        TestUtils.inside(validate_lineup(lineup_multi_bad, all_player_set).toList) {
          case List(ValidationError.WrongNumberOfPlayers, ValidationError.UnknownPlayers) =>
        }
      }
    }
  }
}
