package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object EventUtilsTests extends TestSuite {

  val jumpball_test_cases =
    "19:58:00,0-0,Kavell Bigby-Williams, jumpball lost" ::
    "19:58:00,0-0,Bruno Fernando, jumpball won" ::
    Nil

  val timeout_test_cases =
    "04:04:00,26-33,Ignored, timeout short" ::
    "00:21,59-62,IGNORED 30 Second Timeout" ::
    "10:00,51-60,TEAM 30 Second Timeout" ::
    "10:00,51-60,TEAM Team Timeout" ::
    "10:00,51-60,TEAM Media Timeout" ::
    Nil

  val shot_made_test_cases =
    "08:44:00,20-23,Bruno Fernando1, 2pt dunk 2ndchance;pointsinthepaint made" ::
    "08:44:00,20-23,Bruno Fernando2, 2pt alleyoop pointsinthepaint made" ::
    "08:44:00,20-23,WATKINS,MIKE made Dunk" ::
    "08:44:00,20-23,Jalen Smith, 2pt layup 2ndchance;pointsinthepaint made" ::
    "08:44:00,20-23,Landers Nolley II, 2pt drivinglayup made" ::
    "08:44:00,20-23,BOLTON,RASIR made Layup" ::
    "08:44:00,20-23,STEVENS,LAMAR made Tip In" ::
    "08:44:00,20-23,Eric Ayala, 3pt jumpshot made" ::
    "08:44:00,20-23,SMITH,JALEN made Three Point Jumper" ::
    "15:27,13-8,TRIMBLE JR,BRYAN made Three Point Jumper" ::
    "08:44:00,20-23,Francesca Pan2, 2pt hookshot 2ndchance;pointsinthepaint made" :: //(add more formats because the regex is a bit more complex)
    "08:44:00,20-23,Francesca Pan3, 2pt hookshot pointsinthepaint;fastbreak made" ::
    "08:44:00,20-23,Francesca Pan4, 2pt hookshot pointsinthepaint made" ::
    "08:44:00,20-23,Francesca Pan, 2pt hookshot 2ndchance made" :: //(counts as mid range)
    "08:44:00,20-23,Anthony Cowan, 2pt jumpshot fromturnover;fastbreak made" ::
    "08:44:00,20-23,STEVENS,LAMAR2 made Two Point Jumper" ::
    Nil

  val shot_missed_test_cases =
    "08:44:00,20-23,Bruno Fernando3, 2pt dunk missed" ::
    "08:44:00,20-23,Joshua Tomaic, 2pt alleyoop missed" ::
    "08:44:00,20-23,WATKINS,MIKE1 missed Dunk" ::
    "08:44:00,20-23,Eric Carter, 2pt layup missed" ::
    "08:44:00,20-23,Landers Nolley II2, 2pt drivinglayup;pointsinthepaint missed" ::
    "08:44:00,20-23,TOMAIC,JOSHUA missed Layup" ::
    "08:44:00,20-23,HUERTER,KEVIN missed Tip In" ::
    "08:44:00,20-23,Eric Ayala2, 3pt jumpshot 2ndchance missed" ::
    "08:44:00,20-23,DREAD,MYLES missed Three Point Jumper" ::
    "08:44:00,20-23,Christina Morra2, 2pt hookshot 2ndchance;pointsinthepaint missed" :: //(add more formats because the regex is a bit more complex)
    "08:44:00,20-23,Christina Morra3, 2pt hookshot pointsinthepaint;fastbreak missed" ::
    "08:44:00,20-23,Christina Morra4, 2pt hookshot pointsinthepaint missed" ::
    "08:44:00,20-23,Christina Morra, 2pt hookshot 2ndchance missed" :: //(counts as mid range)
    "08:44:00,20-23,Ricky Lindo Jr., 2pt jumpshot missed" ::
    "08:44:00,20-23,SMITH,JALEN1 missed Two Point Jumper" ::
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

  val tests = Tests {
    "EventUtils" - {
      //TODO: add time-parsing tests, though they are covered by the main test logic
      //TODO: add substitution tests, though they are covered by the main test logic

      // All plays - check the players are pulled out
      "ParseAnyPlay" - {
        //TODO: this isn't a great test because it doesn't tell me if I pull out eg "TEAM 30"...
        // ...instead of "TEAM"

        val some_names = Map(
          "DREAD,MYLES" -> 1,
          "HARRAR,JOHN" -> 1,
          "Kavell Bigby-Williams" -> 1,
          "WATKINS,MIKE" -> 1,
          "Bruno Fernando" -> 1,
          "Emmitt Williams" -> 1,
          "Team" -> 3, //(3 orb)
          "Jalen Smith" -> 1,
          "Darryl Morsell" -> 1,
          "Joshua Tomaic" -> 2, //(missed shot, turnover)
          "SMITH,JALEN" -> 1,
          "TEAM" -> 5, //(3 timeouts, 1 orb, 1 foul)
          "BOLTON,RASIR" -> 1,
          "MASON III,FRANK" -> 1,
          "TRIMBLE JR,BRYAN" -> 1
        )

        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseAnyPlay(name) => name
        }.filter(some_names.keySet).groupBy(identity).mapValues(_.size).toList) {
          case name_counts =>
            name_counts ==> some_names.toList
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
          case List("Team", "TEAM", "TEAM", "TEAM", "TEAM") =>
        }
      }
      // Shots
      val shots_made = List(
        "Bruno Fernando1", "Bruno Fernando2", "WATKINS,MIKE", "Jalen Smith",
        "Landers Nolley II", "BOLTON,RASIR", "STEVENS,LAMAR",
        "Eric Ayala", "SMITH,JALEN", "TRIMBLE JR,BRYAN",
        "Francesca Pan2", "Francesca Pan3", "Francesca Pan4",
        "Francesca Pan", "Anthony Cowan", "STEVENS,LAMAR2"
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
        "Jalen Smith", "Landers Nolley II", "BOLTON,RASIR", "STEVENS,LAMAR",
        "Francesca Pan2", "Francesca Pan3", "Francesca Pan4"
      )
      "ParseRimMade" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseRimMade(name) => name
        }) {
          case `rim_shots_made` =>
        }
      }
      val ft_2p_made = rim_shots_made ++ List(
        "Francesca Pan", "Anthony Cowan", "STEVENS,LAMAR2"
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
        "Landers Nolley II2", "TOMAIC,JOSHUA", "HUERTER,KEVIN",
        "Eric Ayala2", "DREAD,MYLES",
        "Christina Morra2", "Christina Morra3", "Christina Morra4",
        "Christina Morra", "Ricky Lindo Jr.","SMITH,JALEN1"
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
        "Eric Carter", "Landers Nolley II2", "TOMAIC,JOSHUA", "HUERTER,KEVIN",
        "Christina Morra2", "Christina Morra3", "Christina Morra4"
      )
      "ParseRimMissed" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseRimMissed(name) => name
        }) {
          case  `rim_missed` =>
        }
      }
      val fg_2p_missed = rim_missed ++ List(
        "Christina Morra", "Ricky Lindo Jr.", "SMITH,JALEN1"
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
      "ParseLiveOffensiveRebound" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseLiveOffensiveRebound(name) => name
        }) {
          case List(
          "Jalen Smith1", "Team", "SMITH,JALEN2", //(like above but missing that one deadball rebound)
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

      val fts_made_gen2 = List(
        ("Kevin Anderson0M", 1, 1), ("Kevin Anderson1M", 1, 2),
        ("Kevin Anderson", 2, 2), ("Kevin Anderson3M", 1, 3),
      )
      val fts_missed_gen2 = List(
        ("Kevin Anderson0m", 1, 1), ("Kevin Anderson1", 1, 2), ("Kevin Anderson2", 2, 2),
        ("Kevin Anderson3", 1, 3), ("Kevin Anderson4", 2, 3), ("Kevin Anderson5", 3, 3)
      )
      val fts_attempt_gen2 = fts_made_gen2 ++ fts_missed_gen2
      "ParseFreeThrowEventAttemptGen2" - {
        TestUtils.inside(all_test_cases.collect {
          case EventUtils.ParseFreeThrowEventAttemptGen2(name) => name
        }) {
          case `fts_attempt_gen2` =>
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
