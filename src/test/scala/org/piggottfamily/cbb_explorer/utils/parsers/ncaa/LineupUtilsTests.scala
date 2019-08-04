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
          LineupEvent.RawGameEvent(Some("team1.1"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, Some("substitution out"), Some(0), None) ::
          LineupEvent.RawGameEvent(Some("team1.2"), None, Some(0), None) ::
          LineupEvent.RawGameEvent(None, Some("opp1.1"), None, Some(0)) ::
          LineupEvent.RawGameEvent(None, Some("substitution in"), None, Some(0)) ::
          LineupEvent.RawGameEvent(Some("team2.1"), None, Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("leaves game"), Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("enters game"), Some(1), None) ::
          LineupEvent.RawGameEvent(None, Some("opp2.1"), None, Some(1)) ::
          LineupEvent.RawGameEvent(Some("team3.1"), None, Some(2), None) ::
          Nil
        TestUtils.inside(calculate_possessions(test_events_1)) {
          case (3, 2, events) =>
            events ==> test_events_1.map(e => e.copy(
              team_possession = e.team_possession.map(_ + 1),
              opponent_possession = e.opponent_possession.map(_ + 1),
            ))
        }
        val test_events_2 = LineupEvent.RawGameEvent(None, Some("opp1.1")) :: Nil
        TestUtils.inside(calculate_possessions(test_events_2)) {
          case (0, 1, events) =>
            events.map(_.copy(team_possession = Some(1)))
        }
      }
    }
  }
}
