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

      val jumpball_test_cases =
        "19:58:00,0-0,Kavell Bigby-Williams, jumpball lost" ::
        "19:58:00,0-0,Bruno Fernando, jumpball won" ::
        Nil

      val timeout_test_cases =
        "04:04:00,26-33,Ignored, timeout short" ::
        "00:21,59-62,IGNORED 30 Second Timeout" ::
        Nil

      val shot_made_test_cases =
        "08:44:00,20-23,Bruno Fernando, 2pt dunk 2ndchance;pointsinthepaint made" ::
        "08:44:00,20-23,Bruno Fernando, 2pt alleyoop pointsinthepaint made" ::
        "08:44:00,20-23,WATKINS,MIKE made Dunk" ::
        "08:44:00,20-23,Jalen Smith, 2pt layup 2ndchance;pointsinthepaint made" ::
        "08:44:00,20-23,BOLTON,RASIR made Layup" ::
        "08:44:00,20-23,STEVENS,LAMAR made Tip In" ::
        "08:44:00,20-23,Anthony Cowan, 2pt jumpshot fromturnover;fastbreak made" ::
        "08:44:00,20-23,STEVENS,LAMAR made Two Point Jumper" ::
        "08:44:00,20-23,Eric Ayala, 3pt jumpshot made" ::
        "08:44:00,20-23,SMITH,JALEN made Three Point Jumper" ::
        Nil

      val shot_missed_test_cases =
        "08:44:00,20-23,Bruno Fernando, 2pt dunk missed" ::
        "08:44:00,20-23,WATKINS,MIKE missed Dunk" ::
        "08:44:00,20-23,Eric Carter, 2pt layup missed" ::
        "08:44:00,20-23,TOMAIC,JOSHUA missed Layup" ::
        "08:44:00,20-23,Ricky Lindo Jr., 2pt jumpshot missed" ::
        "08:44:00,20-23,SMITH,JALEN missed Two Point Jumper" ::
        "08:44:00,20-23,Eric Ayala, 3pt jumpshot 2ndchance missed" ::
        "08:44:00,20-23,DREAD,MYLES missed Three Point Jumper" ::
        Nil

      val rebound_test_cases =
        "08:44:00,20-23,Darryl Morsell, rebound defensive" ::
        "08:44:00,20-23,Jalen Smith, rebound offensive" ::
        "08:44:00,20-23,Team, rebound offensive team" ::
        "08:44:00,20-23,SMITH,JALEN Offensive Rebound" ::
        "08:44:00,20-23,HARRAR,JOHN Defensive Rebound" ::
        Nil

      val free_throw_made_test_cases =
        "08:44:00,20-23,Kevin Anderson, freethrow 2of2 made" ::
        "08:44:00,20-23,DREAD,MYLES made Free Throw" ::
        Nil

      val free_throw_missed_test_cases =
        "08:44:00,20-23,Kevin Anderson, freethrow 1of2 missed" ::
        "08:44:00,20-23,DREAD,MYLES missed Free Throw" ::
        Nil

      val turnover_test_cases =
        "14:11:00,7-9,Bruno Fernando, turnover badpass" ::
        "14:11:00,7-9,Joshua Tomaic, turnover lostball" ::
        "14:11:00,7-9,Jalen Smith, turnover offensive" ::
        "14:11:00,7-9,Kevin Anderson, turnover travel" ::
        "14:11:00,7-9,MORSELL,DARRYL Turnover" ::
        Nil

      val blocked_test_cases =
        "14:11:00,7-9,Emmitt Williams, block" ::
        "04:53,55-69,LAYMAN,JAKE Blocked Shot" ::
        Nil

      val stolen_test_cases =
        "08:44:00,20-23,Jacob Cushing, steal" ::
        "05:10,55-68,MASON III,FRANK Steal" ::
        Nil

      val foul_test_cases =
        "13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow" ::
        "10:00,51-60,MYKHAILIUK,SVI Commits Foul" ::
        "06:43:00,55-79,Bruno Fernando, foul technical classa;2freethrow" ::
        "02:28:00,27-38,Jalen Smith, foulon" ::
        Nil

      val all_test_cases = (
        jumpball_test_cases ++
        timeout_test_cases ++
        shot_made_test_cases ++
        shot_missed_test_cases ++
        rebound_test_cases ++
        free_throw_made_test_cases ++
        free_throw_missed_test_cases ++
        turnover_test_cases ++
        blocked_test_cases ++
        stolen_test_cases ++
        foul_test_cases
      )

      // jumpball
      "ParseJumpballWonOrLost" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseJumpballWonOrLost(name) => name
        }) {
          case List("Kavell Bigby-Williams", "Bruno Fernando") =>
        }
      }
      // Timeout
      "ParseTimeout" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseTimeout(name) => name
        }) {
          case List("Team", "TEAM") =>
        }
      }
      // Shots
      "ParseShotMade" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseShotMade(name) => name
        }) {
          case List(
            "Bruno Fernando", "Bruno Fernando", "WATKINS,MIKE", "Jalen Smith",
            "BOLTON,RASIR", "STEVENS,LAMAR", "Anthony Cowan", "STEVENS,LAMAR",
            "Eric Ayala", "SMITH,JALEN"
          ) =>
        }

      }
      "ParseShotMissed" - {
          TestUtils.inside(all_test_cases.collect {
            case EventUtils.ParseShotMissed(name) => name
          }) {
            case List(
              "Bruno Fernando", "WATKINS,MIKE", "Eric Carter",
              "TOMAIC,JOSHUA", "Ricky Lindo Jr.", "SMITH,JALEN",
              "Eric Ayala", "DREAD,MYLES"
            ) =>
          }
      }

      // Rebounds
      "ParseRebound" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseRebound(name) => name
        }) {
          case List(
            "Darryl Morsell", "Jalen Smith", "Team", "SMITH,JALEN", "HARRAR,JOHN"
          ) =>
        }
      }

      // Free throws
      "ParseFreeThrowMade" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFreeThrowMade(name) => name
        }) {
          case List(
            "Kevin Anderson", "DREAD,MYLES"
          ) =>
        }
      }
      "ParseFreeThrowMissed" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFreeThrowMissed(name) => name
        }) {
          case List(
            "Kevin Anderson", "DREAD,MYLES"
          ) =>
        }
      }

      // Turnovers
      "ParseTurnover" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseTurnover(name) => name
        }) {
          case List(
            "Bruno Fernando", "Joshua Tomaic", "Jalen Smith", "Kevin Anderson", "MORSELL,DARRYL"
          ) =>
        }
      }

      // Blocks
      "ParseShotBlocked" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseShotBlocked(name) => name
        }) {
          case List("Emmitt Williams", "LAYMAN,JAKE") =>
        }
      }
      // Steals
      "ParseStolen" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseStolen(name) => name
        }) {
          case List("Jacob Cushing", "MASON III,FRANK") =>
        }
      }

      // Fouls
      "ParsePersonalFoul" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParsePersonalFoul(name) => name
        }) {
          case List("Jalen Smith", "MYKHAILIUK,SVI") =>
        }
      }
      "ParseTechnicalFoul" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseTechnicalFoul(name) => name
        }) {
          case List("Bruno Fernando") =>
        }
      }
      "ParseFoulInfo" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFoulInfo(name) => name
        }) {
          case List("Jalen Smith") =>
        }
      }
    }
  }
}
