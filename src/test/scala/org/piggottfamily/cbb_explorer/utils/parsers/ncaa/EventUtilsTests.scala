package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object EventUtilsTests extends TestSuite {

  val tests = Tests {
    "EventUtils" - {
      //TODO: add time-parsing tests, though they are covered by the main test logic
      //TODO: add substitution tests, though they are covered by the main test logic

      // jumpball
      "ParseJumpballWonOrLost" - {
        TestUtils.inside(Some("19:58:00,0-0,Kavell Bigby-Williams, jumpball lost")) {
          case EventUtils.ParseJumpballWonOrLost("Kavell Bigby-Williams") =>
        }
        TestUtils.inside(Some("19:58:00,0-0,Bruno Fernando, jumpball won")) {
          case EventUtils.ParseJumpballWonOrLost("Bruno Fernando") =>
        }
      }
      // Blocks
      "ParseShotBlocked" - {
        TestUtils.inside(Some("14:11:00,7-9,Emmitt Williams, block")) {
          case EventUtils.ParseShotBlocked("Emmitt Williams") =>
        }
        TestUtils.inside(Some("04:53,55-69,LAYMAN,JAKE Blocked Shot")) {
          case EventUtils.ParseShotBlocked("LAYMAN,JAKE") =>
        }
      }
      // Fouls
      "ParsePersonalFoul" - {
        TestUtils.inside(Some("13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow")) {
          case EventUtils.ParsePersonalFoul("Jalen Smith") =>
        }
        TestUtils.inside(Some("10:00,51-60,MYKHAILIUK,SVI Commits Foul")) {
          case EventUtils.ParsePersonalFoul("MYKHAILIUK,SVI") =>
        }
      }
      "ParseTechnicalFoul" - {
        TestUtils.inside(Some("06:43:00,55-79,Bruno Fernando, foul technical classa;2freethrow")) {
          case EventUtils.ParseTechnicalFoul("Bruno Fernando") =>
        }
      }
    }
  }
}
