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
        "08:44:00,20-23,Bruno Fernando1, 2pt dunk 2ndchance;pointsinthepaint made" ::
        "08:44:00,20-23,Bruno Fernando2, 2pt alleyoop pointsinthepaint made" ::
        "08:44:00,20-23,WATKINS,MIKE made Dunk" ::
        "08:44:00,20-23,Jalen Smith, 2pt layup 2ndchance;pointsinthepaint made" ::
        "08:44:00,20-23,BOLTON,RASIR made Layup" ::
        "08:44:00,20-23,STEVENS,LAMAR made Tip In" ::
        "08:44:00,20-23,Anthony Cowan, 2pt jumpshot fromturnover;fastbreak made" ::
        "08:44:00,20-23,STEVENS,LAMAR2 made Two Point Jumper" ::
        "08:44:00,20-23,Eric Ayala, 3pt jumpshot made" ::
        "08:44:00,20-23,SMITH,JALEN made Three Point Jumper" ::
        "15:27,13-8,TRIMBLE JR,BRYAN made Three Point Jumper" ::
        Nil

      val shot_missed_test_cases =
        "08:44:00,20-23,Bruno Fernando3, 2pt dunk missed" ::
        "08:44:00,20-23,Joshua Tomaic, 2pt alleyoop missed" ::
        "08:44:00,20-23,WATKINS,MIKE1 missed Dunk" ::
        "08:44:00,20-23,Eric Carter, 2pt layup missed" ::
        "08:44:00,20-23,TOMAIC,JOSHUA missed Layup" ::
        "08:44:00,20-23,HUERTER,KEVIN missed Tip In" ::
        "08:44:00,20-23,Ricky Lindo Jr., 2pt jumpshot missed" ::
        "08:44:00,20-23,SMITH,JALEN1 missed Two Point Jumper" ::
        "08:44:00,20-23,Eric Ayala2, 3pt jumpshot 2ndchance missed" ::
        "08:44:00,20-23,DREAD,MYLES missed Three Point Jumper" ::
        Nil

      val rebound_test_cases =
        "08:44:00,20-23,Darryl Morsell, rebound defensive" ::
        "08:44:00,20-23,Jalen Smith1, rebound offensive" ::
        "08:44:00,20-23,Team, rebound offensive team" ::
        "08:44:00,20-23,SMITH,JALEN2 Offensive Rebound" ::
        "08:44:00,20-23,HARRAR,JOHN Defensive Rebound" ::
        "04:33,46-45,TEAM Deadball Rebound" ::
        "04:28:0,52-59,Team, rebound offensivedeadball" ::
        "04:28:0,52-59,Team, rebound defensivedeadball" ::
        Nil

      val free_throw_made_test_cases =
        "08:44:00,20-23,Kevin Anderson0M, freethrow 1of1 made" ::
        "08:44:00,20-23,Kevin Anderson1M, freethrow 1of2 made" ::
        "08:44:00,20-23,Kevin Anderson, freethrow 2of2 made" ::
        "08:44:00,20-23,Kevin Anderson3M, freethrow 1of3 made" ::
        "08:44:00,20-23,DREAD,MYLES1 made Free Throw" ::
        Nil

      val free_throw_missed_test_cases =
        "08:44:00,20-23,Kevin Anderson0m, freethrow 1of1 missed" ::
        "08:44:00,20-23,Kevin Anderson1, freethrow 1of2 missed" ::
        "08:44:00,20-23,Kevin Anderson2, freethrow 2of2 missed" ::
        "08:44:00,20-23,Kevin Anderson3, freethrow 1of3 missed" ::
        "08:44:00,20-23,Kevin Anderson4, freethrow 2of3 missed" ::
        "08:44:00,20-23,Kevin Anderson5, freethrow 3of3 missed" ::
        "08:44:00,20-23,DREAD,MYLES2 missed Free Throw" ::
        Nil

      val turnover_test_cases =
        "14:11:00,7-9,Bruno Fernando4, turnover badpass" ::
        "14:11:00,7-9,Joshua Tomaic, turnover lostball" ::
        "14:11:00,7-9,Jalen Smith2, turnover offensive" ::
        "14:11:00,7-9,Kevin Anderson6, turnover travel" ::
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

      val assist_test_cases =
        "18:28:00,0-0,Kyle Guy, assist" ::
        "19:49,0-2,EDWARDS,CARSEN Assist" ::
        Nil

      val foul_test_cases =
        "10:00,51-60,TEAM Commits Foul" :: //(old style tech)
        "13:36:00,7-9,Jalen Smith3, foul personal shooting;2freethrow" ::
        "10:00,51-60,MYKHAILIUK,SVI Commits Foul" ::
        "06:43:00,55-79,Bruno Fernando5, foul technical classa;2freethrow" ::
        "02:28:00,27-38,Jalen Smith4, foulon" ::
        "03:42:00,10-10,Eric Carter1, foul personal flagrant1;2freethrow" ::
        "02:28:00,27-38,Eric Ayala3, foul offensive" ::
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
        assist_test_cases ++
        foul_test_cases
      )

      // All plays - check the players are pulled out
      "ParseAnyPlay" - {

        val some_names = Set(
          "DREAD,MYLES",
          "HARRAR,JOHN",
          "Kavell Bigby-Williams",
          "WATKINS,MIKE",
          "Bruno Fernando",
          "Emmitt Williams",
          "Team",
          "Jalen Smith",
          "Darryl Morsell",
          "Joshua Tomaic",
          "SMITH,JALEN",
          "TEAM",
          "BOLTON,RASIR",
          "MASON III,FRANK",
          "TRIMBLE JR,BRYAN"
        )

        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseAnyPlay(name) => name
        }.filter(some_names).toSet) {
          case `some_names` =>
        }
      }

      // jumpball
      "ParseJumpballWonOrLost" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseJumpballWonOrLost(name) => name
        }) {
          case List("Kavell Bigby-Williams", "Bruno Fernando") =>
        }
      }
      "ParseJumpballWon" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseJumpballWon(name) => name
        }) {
          case List("Bruno Fernando") =>
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
      val shots_made = List(
        "Bruno Fernando1", "Bruno Fernando2", "WATKINS,MIKE", "Jalen Smith",
        "BOLTON,RASIR", "STEVENS,LAMAR", "Anthony Cowan", "STEVENS,LAMAR2",
        "Eric Ayala", "SMITH,JALEN", "TRIMBLE JR,BRYAN"
      )
      "ParseShotMade" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseShotMade(name) => name
        }) {
          case `shots_made` =>
        }
      }
      // Sub-categories
      val rim_shots_made = List(
        "Bruno Fernando1", "Bruno Fernando2", "WATKINS,MIKE",
        "Jalen Smith", "BOLTON,RASIR", "STEVENS,LAMAR"
      )
      "ParseRimMade" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseRimMade(name) => name
        }) {
          case `rim_shots_made` =>
        }
      }
      val ft_2p_made = rim_shots_made ++ List(
        "Anthony Cowan", "STEVENS,LAMAR2"
      )
      "ParseTwoPointerMade" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseTwoPointerMade(name) => name
        }) {
          case `ft_2p_made` =>
        }
      }
      "ParseThreePointerMade" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseThreePointerMade(name) => name
        }) {
          case List(
            "Eric Ayala", "SMITH,JALEN", "TRIMBLE JR,BRYAN"
          ) =>
        }
      }

      val shots_missed = List(
        "Bruno Fernando3", "Joshua Tomaic", "WATKINS,MIKE1", "Eric Carter",
        "TOMAIC,JOSHUA", "HUERTER,KEVIN", "Ricky Lindo Jr.", "SMITH,JALEN1",
        "Eric Ayala2", "DREAD,MYLES"
      )
      "ParseShotMissed" - {
          TestUtils.inside(all_test_cases.collect {
            case EventUtils.ParseShotMissed(name) => name
          }) {
            case `shots_missed` =>
          }
      }
      // Sub-categories
      val rim_missed = List(
        "Bruno Fernando3", "Joshua Tomaic", "WATKINS,MIKE1",
        "Eric Carter", "TOMAIC,JOSHUA", "HUERTER,KEVIN"
      )
      "ParseRimMissed" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseRimMissed(name) => name
        }) {
          case  `rim_missed` =>
        }
      }
      val fg_2p_missed = rim_missed ++ List(
        "Ricky Lindo Jr.", "SMITH,JALEN1"
      )
      "ParseTwoPointerMissed" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseTwoPointerMissed(name) => name
        }) {
          case `fg_2p_missed` =>
        }
      }
      "ParseThreePointerMissed" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseThreePointerMissed(name) => name
        }) {
          case List("Eric Ayala2", "DREAD,MYLES") =>
        }
      }
      // Rebounds
      "ParseRebound" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseRebound(name) => name
        }) {
          case List(
            "Darryl Morsell", "Jalen Smith1", "Team", "SMITH,JALEN2", "HARRAR,JOHN", "TEAM", "Team", "Team"
          ) =>
        }
      }
      //(defensive)
      "ParseOffensiveRebound" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseOffensiveRebound(name) => name
        }) {
          case List(
          "Jalen Smith1", "Team", "SMITH,JALEN2", "Team"
          ) =>
        }
      }
      //(defensive)
      val drbs = List(
        "Darryl Morsell", "HARRAR,JOHN", "Team"
      )
      "ParseDefensiveRebound" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseDefensiveRebound(name) => name
        }) {
          case `drbs` =>
        }
      }
      // (deadball)
      "ParseDeadballRebound" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseDeadballRebound(name) => name
        }) {
          case List(
            "TEAM", "Team", "Team"
          ) =>
        }
      }
      "ParseDeadballRebound" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseOffensiveDeadballRebound(name) => name
        }) {
          case List(
            "Team",
          ) =>
        }
      }

      // Free throws
      val fts_made = List(
        "Kevin Anderson0M", "Kevin Anderson1M", "Kevin Anderson", "Kevin Anderson3M", "DREAD,MYLES1"
      )
      "ParseFreeThrowMade" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFreeThrowMade(name) => name
        }) {
          case `fts_made` =>
        }
      }
      val fts_missed = List(
        "Kevin Anderson0m",
        "Kevin Anderson1", "Kevin Anderson2", "Kevin Anderson3", "Kevin Anderson4",
        "Kevin Anderson5", "DREAD,MYLES2"
      )
      "ParseFreeThrowMissed" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFreeThrowMissed(name) => name
        }) {
          case `fts_missed` =>
        }
      }
      "ParseFreeThrowEvent" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFreeThrowEvent(name) => name
        }) {
          case List(
            "Kevin Anderson0M", "Kevin Anderson1M", "Kevin Anderson3M", "DREAD,MYLES1",
            "Kevin Anderson0m", "Kevin Anderson1", "Kevin Anderson3", "DREAD,MYLES2"
          ) =>
        }
      }
      val fts_attempt = fts_made ++ fts_missed
      "ParseFreeThrowAttempt" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFreeThrowAttempt(name) => name
        }) {
          case `fts_attempt` =>
        }
      }

      // Turnovers
      val turnovers = List(
        "Bruno Fernando4", "Joshua Tomaic", "Jalen Smith2", "Kevin Anderson6", "MORSELL,DARRYL"
      )
      "ParseTurnover" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseTurnover(name) => name
        }) {
          case `turnovers` =>
        }
      }

      // Blocks
      val blockers = List("Emmitt Williams", "LAYMAN,JAKE")
      "ParseShotBlocked" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseShotBlocked(name) => name
        }) {
          case `blockers` =>
        }
      }
      // Steals
      val stealers = List("Jacob Cushing", "MASON III,FRANK")
      "ParseStolen" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseStolen(name) => name
        }) {
          case `stealers` =>
        }
      }

      // Assists
      "ParseAssist" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseAssist(name) => name
        }) {
          case List("Kyle Guy", "EDWARDS,CARSEN") =>
        }
      }

      // Fouls
      "ParsePersonalFoul" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParsePersonalFoul(name) => name
        }) {
          case List("Jalen Smith3", "MYKHAILIUK,SVI", "Eric Carter1") =>
        }
      }
      "ParseFlagrantFoul" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFlagrantFoul(name) => name
        }) {
          case List("Eric Carter1") =>
        }
      }
      "ParseTechnicalFoul" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseTechnicalFoul(name) => name
        }) {
          case List("TEAM", "Bruno Fernando5") =>
        }
      }
      "ParseOffensiveFoul" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseOffensiveFoul(name) => name
        }) {
          case List("Eric Ayala3") =>
        }
      }
      "ParseFoulInfo" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFoulInfo(name) => name
        }) {
          case List("Jalen Smith4") =>
        }
      }
      // combos
      val offensive_actions = shots_made ++ shots_missed ++ fts_attempt ++ turnovers
      "ParseOffensiveEvent" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseOffensiveEvent(name) => name
        }) {
          case `offensive_actions` =>
        }
      }
      val defensive_actions = blockers ++ stealers
      "ParseDefensiveInfoEvent" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseDefensiveInfoEvent(name) => name
        }) {
          case `defensive_actions` =>
        }
      }
      "ParseDefensiveActionEvent" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseDefensiveActionEvent(name) => name
        }) {
          case `drbs` =>
        }
      }
      val defensive_events = drbs ++ defensive_actions
      "ParseDefensiveEvent" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseDefensiveEvent(name) => name
        }) {
          case `defensive_events` =>
        }
      }

    }
  }
}
