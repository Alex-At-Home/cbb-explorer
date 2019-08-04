package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object LineupUtilsTests extends TestSuite with LineupUtils {

  val tests = Tests {
    "LineupUtils" - {
      "calculate_possessions" - {
        val test_events_1 = //(set expected possession -1)
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,player, jumpball lost"), None, None, None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,player, jumpball won"), None, None) ::
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,team1.1"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,player, substitution out"), Some(0), None) ::
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,team1.2"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,opp1.1"), None, Some(0)) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,player, substitution in"), None, Some(0)) ::
          LineupEvent.RawGameEvent(Some("19:58:00,0-0, team2.1"), None, Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,PLAYER Leaves Game"), Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,PLAYER Enters Game"), Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,opp2.1"), None, Some(1)) ::
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,team3.1"), None, Some(2), None) ::
          Nil
        TestUtils.inside(calculate_possessions(test_events_1)) {
          case (3, 2, events) =>
            events ==> test_events_1.map(e => e.copy(
              team_possession = e.team_possession.map(_ + 1),
              opponent_possession = e.opponent_possession.map(_ + 1),
            ))
        }
        
        val test_events_2 = LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,opp1.1")) :: Nil
        TestUtils.inside(calculate_possessions(test_events_2)) {
          case (0, 1, events) =>
            events.map(_.copy(team_possession = Some(1)))
        }

        val jumpball_event = Some("19:58:00,0-0,player, jumpball lost")
        val block_event = Some("19:58:00,0-0,player Blocked Shot")
        val personal_foul_event = Some("19:58:00,0-0,player, foul personal; info")
        val technical_foul_event = Some("19:58:00,0-0,player, foul technical classa; info")
        val test_events_3 = //(set expected possession -1)
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,player, team1.1"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, jumpball_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, block_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, personal_foul_event, Some(0), None) ::
          LineupEvent.RawGameEvent(None, technical_foul_event, Some(0), None) ::
          LineupEvent.RawGameEvent(Some("19:58:00,0-0,player, team1.2"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,player, oppo1.1"), None, Some(0)) ::
          LineupEvent.RawGameEvent(jumpball_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(block_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(personal_foul_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(technical_foul_event, None, None, Some(0)) ::
          LineupEvent.RawGameEvent(None, Some("19:58:00,0-0,player, oppo1.2"), None, Some(0)) ::
          Nil
        TestUtils.inside(calculate_possessions(test_events_3)) {
          case (1, 1, events) =>
            events ==> test_events_3.map(e => e.copy(
              team_possession = e.team_possession.map(_ + 1),
              opponent_possession = e.opponent_possession.map(_ + 1),
            ))
        }

      }
    }
  }
}
