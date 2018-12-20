package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object ExtractorUtilsTests extends TestSuite {
  import ExtractorUtils._


  val tests = Tests {
    "ExtractorUtils" - {

      "build_player_code" - {
        val test_name =
          "Surname, Firstname A B Iiii Iiiiaiii Jr Jr. Sr Sr. 4test the First second rAbbit Third"
        TestUtils.inside(build_player_code(test_name)) {
          case LineupEvent.PlayerCodeId("FiRaSu", PlayerId(`test_name`)) =>
        }
      }
      "build_partial_lineup_list" - {
        val now = new DateTime()
        val all_players @ (player1 :: player2 :: player3 :: player4 :: player5 ::
          player6 :: player7 :: Nil) = List(
            LineupEvent.PlayerCodeId("PlOn", PlayerId("Player One")),
            LineupEvent.PlayerCodeId("PlTw", PlayerId("Player Two")),
            LineupEvent.PlayerCodeId("PlTh", PlayerId("Player Three")),
            LineupEvent.PlayerCodeId("PlFo", PlayerId("Player Four")),
            LineupEvent.PlayerCodeId("PlFi", PlayerId("Player Five")),
            LineupEvent.PlayerCodeId("PlSi", PlayerId("Player Six")),
            LineupEvent.PlayerCodeId("PlSe", PlayerId("Player Seven")),
          )
        val my_team = TeamSeasonId(TeamId("TestTeam1"), Year(2018))
        val other_team = TeamSeasonId(TeamId("TestTeam2"), Year(2017))
        val box_lineup = LineupEvent(
          date = now,
          start_min = 0.0,
          end_min = -100.0,
          duration_mins = 0.0,
          score_diff = 0,
          team = my_team,
          opponent = other_team,
          lineup_id = LineupEvent.LineupId.unknown,
          players = all_players,
          players_in = Nil,
          players_out = Nil,
          raw_team_events = Nil,
          raw_opponent_events = Nil,
          team_stats = LineupEventStats.empty,
          opponent_stats = LineupEventStats.empty
        )
        val starting_lineup = box_lineup.copy(players = box_lineup.players.take(5))
        val test_events =
          // First event - sub immediately after game start
          Model.SubInEvent(0.1, player6.id.name) ::
          Model.SubOutEvent(0.1, player1.id.name) ::
          Model.OtherTeamEvent(0.2, "event1a") ::
          Model.OtherTeamEvent(0.2, "event2a") ::
          // Second event
          Model.SubInEvent(0.4, player1.id.name) ::
          // confirm that all upper case names are returned to normal form:
          Model.SubInEvent(0.4, player7.id.name.toUpperCase) ::
          // CHECK: we only care about "code" not "id":
          Model.SubOutEvent(0.4, player2.id.name.toUpperCase + " ii") ::
          Model.SubOutEvent(0.4, player4.id.name) ::
          Model.OtherOpponentEvent(0.4, "event1b") ::
          Model.OtherOpponentEvent(0.4, "event2b") ::
          Model.OtherTeamEvent(0.4, "event3a") ::
          Model.OtherTeamEvent(0.4, "event4a") ::
          // Half time! (third event)
          Model.GameBreakEvent(20.0) ::
          // (subs happen immediately after break)
          Model.SubOutEvent(20.0, player1.id.name) ::
          Model.OtherOpponentEvent(20.0, "PlayerA Leaves Game") :: //opponents can sub too....
          Model.OtherOpponentEvent(20.0, "PlayerB, substitution in") :: //(new format))
          Model.SubInEvent(20.0, player6.id.name) ::
          // Fourth event  - sub-on-sub action
          Model.SubOutEvent(20.4, player2.id.name) ::
          Model.SubOutEvent(20.4, player4.id.name) ::
          Model.SubInEvent(20.4, player1.id.name) :: // check subs in any order
          Model.SubInEvent(20.4, player7.id.name) ::
          // Overtime! (first event)
          Model.GameBreakEvent(40.0) ::
          // Sixth event
          Model.OtherOpponentEvent(40.4, "event3b") ::
          Model.OtherTeamEvent(40.4, "event5a") ::
          Model.SubInEvent(40.5, player6.id.name) ::
          Model.SubOutEvent(40.5, player1.id.name) ::
          Model.OtherTeamEvent(40.6, "event6a") ::
          Model.OtherOpponentEvent(40.7, "event4b") ::
          // Fin (Seventh event)
          Model.GameEndEvent(45.0) ::
          Nil

        TestUtils.inside(build_partial_lineup_list(test_events.reverse.toIterator, box_lineup)) {
          case List(event_1, event_2, event_3, event_4, event_5, event_6, event_7) =>
            TestUtils.inside(event_1) {
              case LineupEvent(
                `now`, 0.0, 0.1, delta, 0, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(), List(),
                List(), List(),
                _, _
              ) =>
                "%.1f".format(delta) ==> "0.1"
                players ==> starting_lineup.players.sortBy(_.code)
                lineup_id ==> players.map(_.code).mkString("_")
            }
            TestUtils.inside(event_2) {
              case LineupEvent(
                `now`, 0.1, 0.4, delta, 0, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player6`), List(`player1`),
                List("event1a", "event2a"), List(),
                _, _
              ) =>
                "%.1f".format(delta) ==> "0.3"
                players ==>  {
                  event_1.players.toSet + player6 - player1
                }.toList.sortBy(_.code)
                lineup_id ==> players.map(_.code).mkString("_")
            }
            TestUtils.inside(event_3) {
              case LineupEvent(
                `now`, 0.4, 20.0, delta, 0, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player7`, `player1`), List(`player4`, player2_with_mods),
                List("event3a", "event4a"), List("event1b", "event2b"),
                _, _
              ) =>

                player2_with_mods.code ==> `player2`.code // (we corrupted the id)
                "%.1f".format(delta) ==> "19.6"
                players ==>  {
                  event_2.players.toSet + player1 + player7 - player2 - player4
                }.toList.sortBy(_.code)
                lineup_id ==> players.map(_.code).mkString("_")
            }
            TestUtils.inside(event_4) {
              case LineupEvent(
                `now`, 20.0, 20.4, delta, 0, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player6`), List(`player1`),
                List(), List("PlayerA Leaves Game", "PlayerB, substitution in"),
                _, _
              ) =>
                "%.1f".format(delta) ==> "0.4"
                players ==>  {
                  starting_lineup.players.toSet + player6 - player1
                }.toList.sortBy(_.code)
                lineup_id ==> players.map(_.code).mkString("_")
            }
            TestUtils.inside(event_5) {
              case LineupEvent(
                `now`, 20.4, 40.0, delta, 0, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player7`, `player1`), List(`player4`, `player2`),
                List(), List(),
                _, _
              ) =>
                "%.1f".format(delta) ==> "19.6"
                players ==>  {
                  event_5.players.toSet + player1 + player7 - player2 - player4
                }.toList.sortBy(_.code)
                lineup_id ==> players.map(_.code).mkString("_")
            }
            TestUtils.inside(event_6) {
              case LineupEvent(
                `now`, 40.0, 40.5, delta, 0, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(), List(),
                List("event5a"), List("event3b"),
                _, _
              ) =>
                "%.1f".format(delta) ==> "0.5"
                players ==> starting_lineup.players.sortBy(_.code)
                lineup_id ==> players.map(_.code).mkString("_")
            }
            TestUtils.inside(event_7) {
              case LineupEvent(
                `now`, 40.5, 45.0, delta, 0, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(player6), List(player1),
                List("event6a"), List("event4b"),
                _, _
              ) =>
                "%.1f".format(delta) ==> "4.5"
                players ==>  {
                  event_7.players.toSet + player6 - player1
                }.toList.sortBy(_.code)
                lineup_id ==> players.map(_.code).mkString("_")
            }
        }
      }
    }
  }
}
