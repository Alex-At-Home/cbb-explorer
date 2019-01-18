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
          case LineupEvent.PlayerCodeId("FiRaSurname", PlayerId(`test_name`)) =>
        }
        //TODO add some other cases (single name, no space for intermediate)
      }

      "reorder_and_reverse" - {
        // (this is also partially tested by "build_partial_lineup_list" below
        //  but want to demonstrate all the branches of this somewhat complex sub function)

        val event_list @ (event_list_1 :: event_list_2 :: event_list_rest) = List(
          Model.OtherTeamEvent(0.4, Game.Score(0, 0), "pre-sub-1-no-ref"),
          Model.OtherOpponentEvent(0.4, Game.Score(0, 0), "pre-sub-2-no-ref"),
          Model.OtherTeamEvent(0.4, Game.Score(0, 0), "[player1] pre-sub-3-ref-p1"),
          Model.OtherTeamEvent(0.4, Game.Score(0, 0), "[player2] pre-sub-4-ref-p2"),
          Model.OtherOpponentEvent(0.4, Game.Score(0, 0), "[player1] pre-sub-5-ignore-p1"),
          Model.OtherOpponentEvent(0.4, Game.Score(0, 0), "[player2] pre-sub-6-ignore-p2"),

          Model.SubInEvent(0.4, "player1"),
          Model.OtherTeamEvent(0.4, Game.Score(0, 0), "middle-event"),
          Model.SubOutEvent(0.4, "player2"),

          Model.OtherTeamEvent(0.4, Game.Score(0, 0), "post-sub-1-no-ref"),
          Model.OtherOpponentEvent(0.4, Game.Score(0, 0), "post-sub-2-no-ref"),
          Model.OtherTeamEvent(0.4, Game.Score(0, 0), "[player1] post-sub-3-ref-p1"),
          Model.OtherTeamEvent(0.4, Game.Score(0, 0), "[player2] post-sub-4-ref-p2"),
          Model.OtherOpponentEvent(0.4, Game.Score(0, 0), "[player1] post-sub-5-ignore-p1"),
          Model.OtherOpponentEvent(0.4, Game.Score(0, 0), "[player2] post-sub-6-ignore-p2")
        )

        // (check trivial case )
        TestUtils.inside(reorder_and_reverse(event_list.take(2).toIterator)) {
          case event_list_2 :: event_list_1 :: Nil =>
        }

        // (actual logic)
        TestUtils.inside(reorder_and_reverse(event_list.reverse.toIterator)) {
          case reordered_event_list =>
            TestUtils.inside(reordered_event_list.map {
              case ev: Model.MiscGameEvent => ev.event_string
              case sub: Model.SubEvent => sub.player_name
              case other @ _ => other.toString
            }) {
              case List(
                "pre-sub-1-no-ref", "pre-sub-2-no-ref",
                "[player2] pre-sub-4-ref-p2",
                "[player1] pre-sub-5-ignore-p1", "[player2] pre-sub-6-ignore-p2",
                "[player2] post-sub-4-ref-p2",

                "player1", "player2",

                "[player1] pre-sub-3-ref-p1", "middle-event",

                "post-sub-1-no-ref", "post-sub-2-no-ref",
                "[player1] post-sub-3-ref-p1",
                "[player1] post-sub-5-ignore-p1", "[player2] post-sub-6-ignore-p2"
              ) =>
            }
        }
      }

      "build_partial_lineup_list" - {
        val now = new DateTime()
        val all_players @ (player1 :: player2 :: player3 :: player4 :: player5 ::
          player6 :: player7 :: Nil) = List(
            "Player One", "Player Two", "Player Three",
            "Player Four", "Player Five", "Player Six", "Player Seven"
          ).map(build_player_code)

        val my_team = TeamSeasonId(TeamId("TestTeam1"), Year(2017))
        val other_team = TeamSeasonId(TeamId("TestTeam2"), Year(2017))
        val box_lineup = LineupEvent(
          date = now,
          start_min = 0.0,
          end_min = -100.0,
          duration_mins = 0.0,
          score_info = LineupEvent.ScoreInfo.empty,
          team = my_team,
          opponent = other_team,
          lineup_id = LineupEvent.LineupId.unknown,
          players = all_players,
          players_in = Nil,
          players_out = Nil,
          raw_game_events = Nil,
          team_stats = LineupEventStats.empty,
          opponent_stats = LineupEventStats.empty
        )
        val starting_lineup = box_lineup.copy(players = box_lineup.players.take(5))
        val test_events =
          // First event - sub immediately after game start
          Model.SubInEvent(0.1, player6.id.name) ::
          Model.SubOutEvent(0.1, player1.id.name) ::
          Model.OtherTeamEvent(0.2, Game.Score(1, 0), "event1a") ::
          Model.OtherTeamEvent(0.2, Game.Score(2, 0), "event2a") ::
          // Second event
          Model.SubInEvent(0.4, player1.id.name) ::
          // confirm that all upper case names are returned to normal form:
          Model.SubInEvent(0.4, player7.id.name.toUpperCase) ::
          // CHECK: we only care about "code" not "id":
          Model.SubOutEvent(0.4, player2.id.name.toUpperCase + " ii") ::
          Model.SubOutEvent(0.4, player4.id.name) ::
          Model.OtherOpponentEvent(0.4, Game.Score(1, 1), "event1b") ::
          Model.OtherOpponentEvent(0.4, Game.Score(1, 2), "event2b") ::
          Model.OtherTeamEvent(0.4, Game.Score(3, 2), "event3a") ::
          Model.OtherTeamEvent(0.4, Game.Score(4, 2), "event4a") ::
          // Half time! (third event)
          Model.GameBreakEvent(20.0) ::
          // (subs happen immediately after break)
          Model.SubOutEvent(20.0, player1.id.name) ::
          Model.OtherOpponentEvent(20.0, Game.Score(4, 2), "PlayerA Leaves Game") :: //opponents can sub too....
          Model.OtherOpponentEvent(20.0, Game.Score(4, 2), "PlayerB, substitution in") :: //(new format))
          Model.SubInEvent(20.0, player6.id.name) ::
          // Fourth event  - sub-on-sub action
          Model.SubOutEvent(20.4, player2.id.name) ::
          Model.SubOutEvent(20.4, player4.id.name) ::
          Model.SubInEvent(20.4, player1.id.name) :: // check subs in any order
          Model.SubInEvent(20.4, player7.id.name) ::
          // Overtime! (first event)
          Model.GameBreakEvent(40.0) ::
          // Sixth event
          Model.OtherOpponentEvent(40.4, Game.Score(4, 3), "event3b") ::
          Model.OtherTeamEvent(40.4, Game.Score(5, 3), "event5a") ::
          Model.SubInEvent(40.5, player6.id.name) ::
          Model.SubOutEvent(40.5, player1.id.name) ::
          Model.OtherTeamEvent(40.6, Game.Score(6, 3), "event6a") ::
          Model.OtherOpponentEvent(40.7, Game.Score(6, 4) , "event4b") ::
          // Fin (Seventh event)
          Model.GameEndEvent(45.0) ::
          Nil

        TestUtils.inside(build_partial_lineup_list(test_events.reverse.toIterator, box_lineup)) {
          case List(event_1, event_2, event_3, event_4, event_5, event_6, event_7) =>
            TestUtils.inside(event_1) {
              case LineupEvent(
                `now`, 0.0, 0.1, delta, score, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(), List(),
                List(),
                _, _
              ) =>
                "%.1f".format(delta) ==> "0.1"
                score ==> LineupEvent.ScoreInfo(
                  Game.Score(0, 0), Game.Score(0, 0), 0, 0
                )
                lineup_id ==> players.map(_.code).mkString("_")
                players ==> starting_lineup.players.sortBy(_.code)
            }
            TestUtils.inside(event_2) {
              case LineupEvent(
                new_time, 0.1, 0.4, delta, score, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player6`), List(`player1`),
                List(
                  LineupEvent.RawGameEvent(Some("event1a"), None),
                  LineupEvent.RawGameEvent(Some("event2a"), None)
                ),
                _, _
              ) =>
                new_time ==> now.plusMillis(6000)
                "%.1f".format(delta) ==> "0.3"
                score ==> LineupEvent.ScoreInfo(
                  Game.Score(0, 0), Game.Score(2, 0), 0, 2
                )
                lineup_id ==> players.map(_.code).mkString("_")
                players ==>  {
                  event_1.players.toSet + player6 - player1
                }.toList.sortBy(_.code)
            }
            TestUtils.inside(event_3) {
              case LineupEvent(
                _, 0.4, 20.0, delta, score, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player1`, `player7`), List(player2_with_mods, `player4`),
                List(
                  LineupEvent.RawGameEvent(None, Some("event1b")),
                  LineupEvent.RawGameEvent(None, Some("event2b")),
                  LineupEvent.RawGameEvent(Some("event3a"), None),
                  LineupEvent.RawGameEvent(Some("event4a"), None)
                ),
                _, _
              ) =>
                "%.1f".format(delta) ==> "19.6"
                score ==> LineupEvent.ScoreInfo(
                  Game.Score(2, 0), Game.Score(4, 2), 2, 2
                )
                lineup_id ==> players.map(_.code).mkString("_")
                players ==>  {
                  event_2.players.toSet + player1 + player7 - player2 - player4
                }.toList.sortBy(_.code)
                player2_with_mods.code ==> `player2`.code // (we corrupted the id)
            }
            TestUtils.inside(event_4) {
              case LineupEvent(
                _, 20.0, 20.4, delta, score, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player6`), List(`player1`),
                List(
                  LineupEvent.RawGameEvent(None, Some("PlayerA Leaves Game")),
                  LineupEvent.RawGameEvent(None, Some("PlayerB, substitution in"))
                ),
                _, _
              ) =>
                "%.1f".format(delta) ==> "0.4"
                score ==> LineupEvent.ScoreInfo(
                  Game.Score(4, 2), Game.Score(4, 2), 2, 2
                )
                lineup_id ==> players.map(_.code).mkString("_")
                players ==>  {
                  starting_lineup.players.toSet + player6 - player1
                }.toList.sortBy(_.code)
            }
            TestUtils.inside(event_5) {
              case LineupEvent(
                _, 20.4, 40.0, delta, score, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player1`, `player7`), List(`player2`, `player4`),
                List(),
                _, _
              ) =>
                "%.1f".format(delta) ==> "19.6"
                score ==> LineupEvent.ScoreInfo(
                  Game.Score(4, 2), Game.Score(4, 2), 2, 2
                )
                lineup_id ==> players.map(_.code).mkString("_")
                players ==>  {
                  event_4.players.toSet + player1 + player7 - player2 - player4
                }.toList.sortBy(_.code)
            }
            TestUtils.inside(event_6) {
              case LineupEvent(
                _, 40.0, 40.5, delta, score, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(), List(),
                List(
                  LineupEvent.RawGameEvent(None, Some("event3b")),
                  LineupEvent.RawGameEvent(Some("event5a"), None)
                ),
                _, _
              ) =>
                "%.1f".format(delta) ==> "0.5"
                score ==> LineupEvent.ScoreInfo(
                  Game.Score(4, 2), Game.Score(5, 3), 2, 2
                )
                lineup_id ==> players.map(_.code).mkString("_")
                players ==> starting_lineup.players.sortBy(_.code)
            }
            TestUtils.inside(event_7) {
              case LineupEvent(
                _, 40.5, 45.0, delta, score, `my_team`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(player6), List(player1),
                List(
                  LineupEvent.RawGameEvent(Some("event6a"), None),
                  LineupEvent.RawGameEvent(None, Some("event4b"))
                ),
                _, _
              ) =>
                "%.1f".format(delta) ==> "4.5"
                score ==> LineupEvent.ScoreInfo(
                  Game.Score(5, 3), Game.Score(6, 4), 2, 2
                )
                lineup_id ==> players.map(_.code).mkString("_")
                players ==>  {
                  event_7.players.toSet + player6 - player1
                }.toList.sortBy(_.code)
            }
        }
        // Test format change for 2018, builds post-half lineup from pre-half lineup
        // (instead of starting lineup)
        val box_lineup_2018 = box_lineup.copy(team = TeamSeasonId(TeamId("Maryland"), Year(2018)))
        TestUtils.inside(build_partial_lineup_list(test_events.reverse.toIterator, box_lineup_2018)) {
          case List(_, _, event_3, event_4, _, _, _) =>
            val my_team_2018 = box_lineup_2018.team
            TestUtils.inside(event_4) {
              case LineupEvent(
                _, _, _, _, score, `my_team_2018`, `other_team`,
                LineupEvent.LineupId(lineup_id), players,
                List(`player6`), List(`player1`),
                _,
                _, _
              ) =>
                score ==> LineupEvent.ScoreInfo(
                  Game.Score(4, 2), Game.Score(4, 2), 2, 2
                )
                lineup_id ==> players.map(_.code).mkString("_")
                players ==>  {
                  event_3.players.toSet - player1 //(ie lineup only has 4 entries)
                }.toList.sortBy(_.code)
            }
        }
      }
    }
  }
}
