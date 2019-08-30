package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object PossessionUtilsTests extends TestSuite with PossessionUtils {

  val tests = Tests {
    "PossessionUtils" - {

//// TODO:
/*

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
        */
      }
    }
}
