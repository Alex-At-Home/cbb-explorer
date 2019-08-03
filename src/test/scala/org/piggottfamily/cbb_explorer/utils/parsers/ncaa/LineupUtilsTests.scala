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
        val test_events_1 =
          LineupEvent.RawGameEvent(Some("team1.1"), None) ::
          LineupEvent.RawGameEvent(None, Some("substitution out")) ::
          LineupEvent.RawGameEvent(Some("team1.2"), None) ::
          LineupEvent.RawGameEvent(None, Some("opp1.1")) ::
          LineupEvent.RawGameEvent(Some("substitution in"), None) ::
          LineupEvent.RawGameEvent(Some("team2.1"), None) ::
          LineupEvent.RawGameEvent(None, Some("leaves game")) ::
          LineupEvent.RawGameEvent(None, Some("enters game")) ::
          LineupEvent.RawGameEvent(None, Some("opp2.1")) ::
          LineupEvent.RawGameEvent(Some("team3.1"), None) ::
          Nil
        TestUtils.inside(calculate_possessions(test_events_1)) {
          case (3, 2) =>
        }
        val test_events_2 = LineupEvent.RawGameEvent(None, Some("opp1.1")) :: Nil
        TestUtils.inside(calculate_possessions(test_events_2)) {
          case (0, 1) =>
        }
      }
    }
  }
}
