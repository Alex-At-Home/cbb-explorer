package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object PossessionUtilsTests extends TestSuite with PossessionUtils {
  import ExtractorUtils._
  import StateUtils.StateTypes._

  val tests = Tests {
    "PossessionUtils" - {

      val gs = Game.Score(0, 0)

      /** A handy compilation of events */
      object Events {
        val game_break = Model.GameBreakEvent(0.9)
        val jump_won_team = Model.OtherTeamEvent(0.0, gs, 0, "19:58:00,0-0,Bruno Fernando, jumpball won")
        val jump_won_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "19:58:00,0-0,Bruno Fernando, jumpball won")
        val jump_lost_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "19:58:00,0-0,Kavell Bigby-Williams, jumpball lost")

        val turnover_team = Model.OtherTeamEvent(0.0, gs, 0, "08:44:00,20-23,Bruno Fernando, turnover badpass")
        val steal_team = Model.OtherTeamEvent(0.0, gs, 0, "05:10,55-68,MASON III,FRANK Steal")
        val made_team = Model.OtherTeamEvent(0.0, gs, 0, "10:00,51-60,SMITH,JALEN made Three Point Jumper")
        val made_ft_team = Model.OtherTeamEvent(0.0, gs, 0, "05:10,55-68,Kevin Anderson, freethrow 2of2 made")
        val missed_ft_team = Model.OtherTeamEvent(0.0, gs, 0, "10:00,51-60,DREAD,MYLES missed Free Throw")
        val orb_team = Model.OtherTeamEvent(0.0, gs, 0, "10:00,51-60,Darryl Morsell, rebound offensive")
        val drb_team = Model.OtherTeamEvent(0.0, gs, 0, "10:00,51-60,Darryl Morsell, rebound defensive")
        val foul_team = Model.OtherTeamEvent(0.0, gs, 0, "10:00,51-60,MYKHAILIUK,SVI Commits Foul")

        val made_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,SMITH,JALEN made Three Point Jumper")
        val missed_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "02:28:00,27-38,Eric Ayala, 3pt jumpshot 2ndchance missed")
        val foul_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,MYKHAILIUK,SVI Commits Foul")
        val made_ft_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "05:10,55-68,Kevin Anderson, freethrow 2of2 made")
        val missed_ft_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,DREAD,MYLES missed Free Throw")

        val orb_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,Darryl Morsell, rebound offensive")
        val drb_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,Darryl Morsell, rebound defensive")
        val tech_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "06:43:00,55-79,Bruno Fernando, foul technical classa;2freethrow")

        val missed_possession_opponent = Model.OtherOpponentEvent(1.0, gs, 0, s"10:00,51-60, $error_event_string")
          //(linked to drb_opponent)

        val unknown_team = Model.OtherTeamEvent(0.0, gs, 0, "UNKNOWN_EVENT")
      }

      "concurrent_event_handler" - {
        // (test check_for_concurrent_event and rearrange_concurrent_event)

        val test_events @ (
          ev1 :: ev2 :: ev3 :: ev4 :: ev_gb :: ev5 :: ev6 :: ev7
//TODO:
          :: Nil
        ) =
          // Test minutes based clumping
          Model.OtherTeamEvent(0.4, Game.Score(0, 0), 1, "ev-1") ::
          Model.OtherOpponentEvent(0.5, Game.Score(0, 0), 1, "ev-2") ::
          Model.OtherTeamEvent(0.9, Game.Score(0, 0), 1, "ev-3") ::
          Model.OtherOpponentEvent(0.9, Game.Score(0, 0), 1, "ev-4") ::
          Model.GameBreakEvent(0.9) ::
          Model.OtherTeamEvent(0.9, Game.Score(0, 0), 1, "ev-5") ::
          Model.OtherOpponentEvent(0.9, Game.Score(0, 0), 1, "ev-6") ::
          Model.OtherTeamEvent(1.0, Game.Score(0, 0), 1, "ev-7") ::
          // Test possession directing based clumping
//TODO:
          Nil
        val test_events_in = test_events.map(ev => ConcurrentClump(ev :: Nil))


        TestUtils.inside(
          StateUtils.foldLeft(test_events_in, PossState.init, concurrent_event_handler) {
            case StateEvent.Next(ctx, state, ConcurrentClump(evs)) =>
              ctx.stateChange(state, ConcurrentClump(evs))
            case StateEvent.Complete(ctx, _) =>
              ctx.noChange
          }
        ) {
          case FoldStateComplete(_,
            ConcurrentClump(`ev1` :: Nil) ::
            ConcurrentClump(`ev2` :: Nil) ::
            ConcurrentClump(`ev3` :: `ev4` :: Nil) ::
            ConcurrentClump(`ev_gb` :: Nil) ::
            ConcurrentClump(`ev5` :: `ev6` :: Nil) ::
            ConcurrentClump(`ev7` :: Nil) ::
            Nil
          ) =>
        }
      }

      "first_possession_status" - {
        val test_event_pairs =
          (Events.jump_won_team -> Some(Direction.Team)) ::
          (Events.jump_won_opponent -> Some(Direction.Opponent)) ::
          (Events.turnover_team -> Some(Direction.Team)) ::
          (Events.steal_team -> Some(Direction.Opponent)) ::
          (Events.made_team -> Some(Direction.Team)) ::
          (Events.missed_ft_team -> Some(Direction.Team)) ::
          (Events.missed_opponent -> Some(Direction.Opponent)) ::
          (Events.made_ft_opponent -> Some(Direction.Opponent)) ::
          (Events.unknown_team -> None) ::
          Nil

        TestUtils.inside(
          test_event_pairs.zipWithIndex.map(_._2).map { test_index =>
            first_possession_status(test_event_pairs.drop(test_index).map(_._1))
          }
        ) {
          case l =>
            val expected = test_event_pairs.map(_._2)
            if (l != expected) {
              print_useful_list_diff(l, expected)
            }
            l ==> expected
        }
      }

      "clump_possession_status" - {

        val test_event_pairs =
          (Events.drb_opponent.copy(poss = 1) -> PossessionArrowSwitch) :: //(is ignored)
          (Events.game_break -> PossessionArrowSwitch) ::
          (Events.drb_opponent -> PossessionEnd) ::
          (Events.turnover_team -> PossessionEnd) ::
          (Events.tech_opponent -> PossessionContinues) ::
          (Events.made_team -> PossessionContinues) :: //(cos of the missed and one next)
          (Events.missed_ft_team -> PossessionContinues) ::
          (Events.made_team -> PossessionEnd) ::
          (Events.made_ft_team -> PossessionEnd) :: //(no missed FTs follow)
          (Events.made_team -> PossessionEnd) :: //(no FTs after)
          (Events.unknown_team -> PossessionContinues) :: //(no FTs after)
          Nil

        val turnover_events =
          (List(Events.turnover_team) -> PossessionEnd) ::
          (List(Events.foul_team) -> PossessionEnd) ::
          Nil

        val missed_ft_events =
          (List(
              Events.missed_ft_team.copy(score = Game.Score(10, 0), poss = 1), //(ignored)
              Events.made_ft_team.copy(score = Game.Score(1, 0)),
              Events.missed_ft_team.copy(score = Game.Score(0, 0)),
            ) -> PossessionEnd
          ) ::
          (List(
              Events.made_ft_team.copy(score = Game.Score(0, 0)),
              Events.missed_ft_team.copy(score = Game.Score(1, 0)),
            ) -> PossessionContinues
          ) ::
          Nil

        val events_that_error_team = //(check DRBs prevent errors occuring)
          (List(Events.steal_team) -> PossessionError) ::
          (List(Events.missed_opponent) -> PossessionError) ::
          Nil

        val events_that_error_oppo = //(check DRBs prevent errors occuring)
          (List(Events.drb_team, Events.made_team) -> PossessionEnd) ::
          (List(Events.made_team) -> PossessionError) ::
          (List(Events.turnover_team) -> PossessionError) ::
          Nil

        val team_state = PossState.init.copy(direction = Direction.Team)
        val oppo_state = PossState.init.copy(direction = Direction.Opponent)

        sealed trait TestCase
        case class TestCase1[T](s: PossState, l: List[(Model.PlayByPlayEvent, T)]) extends TestCase
        case class TestCase2[T](s: PossState, l: List[(List[Model.PlayByPlayEvent], T)]) extends TestCase

        List(
          TestCase1(team_state, test_event_pairs),
          TestCase2(team_state, turnover_events),
          TestCase2(team_state, missed_ft_events),
          TestCase2(team_state, events_that_error_team),
          TestCase2(oppo_state, events_that_error_oppo)
        ).foreach {
          case TestCase1(s, l) =>
            TestUtils.inside(
              l.zipWithIndex.map(_._2).map { test_index =>
                clump_possession_status(s, l.drop(test_index).map(_._1))
              }
            ) {
              case res =>
                val expected = l.map(_._2)
                if (res != expected) {
                  print_useful_list_diff(res, expected)
                }
                res ==> expected
            }
            case TestCase2(s, ll) => ll.foreach { case (l, expected) =>
              TestUtils.inside(clump_possession_status(s, l)) {
                case res =>
                  if (res != expected) {
                    println("+++++++++++++++++++")
                    println(s"[$l] vs [$res]")
                    println("-------------------")
                  }
                  res ==> expected
              }
            }
        }
      }

      "calculate_possessions" - {

        // Test Cases 1.*: Check that correctly calculates first possession
        // Test Cases 2.*: Check that rebounds are handled correctly (including compound clumps with DRBs)
        // Test Cases 3.*: Check that possession change is handled correctly across a game break
        // Test Cases 4.*: check that missed free throws are handled correctly
        // Test Cases 5.*: Check that after a possession error everything stops

        val game_1_events = //Test cases 1-3
          // Jump ball, ignored (1.*)
          Events.jump_won_team.copy(min = 19.58, poss = -1) ::
          Events.jump_lost_opponent.copy(min = 19.58, poss = -1) ::
          // 1T possession - team wins jump (possession arrow to opponent) and scores (1.*)
          Events.made_team.copy(min = 19.0, poss = -1) ::
          // 1O possession - DRBs and ORBs (1.*)
          Events.missed_opponent.copy(min = 18.0, poss = -1) ::
          Events.orb_opponent.copy(min = 17.0, poss = -1) ::
          Events.missed_opponent.copy(min = 16.0, poss = -1) ::
          Events.drb_team.copy(min = 16.0, poss = -1) :: //(DRB belongs to end of offensive possession)
          // 2T possession (note compound clump)
          Events.made_team.copy(min = 14.0, poss = -2) ::
          // 2O possession
          Events.made_opponent.copy(min = 13.0, poss = -2) ::
          // Game break (3.*)
          Events.game_break.copy(min = 10.0) ::
          // 3O possession
          Events.made_opponent.copy(min = 20.0, poss = -3) ::
          // Game end (check is ignored)
          Events.game_break.copy(min = 0.0) ::
          Nil

        val gs1 = Game.Score(0, 1)
        val gs2 = Game.Score(1, 0)

        val game_2_events = //Test cases 1-5
          // 1O possession - fouled and made and one (1,*, 2.*, 4.*)
          Events.made_opponent.copy(min = 19.0, poss = -1) ::
          Events.made_ft_opponent.copy(min = 19.0, poss = -1) ::
          // 1T possession - fouled and both free throws made (2.*, 4.*)
          Events.made_ft_team.copy(min = 18.0, poss = -1) ::
          Events.made_ft_team.copy(min = 18.0, poss = -1) ::
          // 2O incomplete possession (3.*)
          Model.OtherOpponentEvent(17.0, gs, -2, "17:00,0-0,Misc event") ::
          // Game break (3.*)
          Events.game_break.copy(min = 15.0) ::
          // 2T possession - fouled, missed a free throw, ends up it was the first so possession switches (2.*, 4.*)
          Events.made_ft_team.copy(min = 14.0, score = gs1, poss = -2) ::
          Events.missed_ft_team.copy(min = 14.0, poss = -2) ::
          // 3O possession - missed both free throws, ORB so possession continues (2.*, 4.*)
          Events.missed_ft_opponent.copy(min = 13.0, poss = -3) ::
          Events.missed_ft_opponent.copy(min = 13.0, poss = -3) ::
          Events.orb_opponent.copy(min = 12.0, poss = -3) ::
          // 3O possession continues - missed a free throw, this time it was the 2nd, ORB so possession continues (2.*, 4.*)
          Events.missed_ft_opponent.copy(min = 11.0, score = gs2, poss = -3) ::
          Events.made_ft_opponent.copy(min = 11.0, poss = -3) ::
          Events.orb_opponent.copy(min = 10.0, poss = -3) ::
          // 3O possession continues - missed a free throw, this time it was the 2nd, DRB so possession switches (2.*, 4.*)
          Events.missed_ft_opponent.copy(min = 9.0, score = gs2, poss = -3) ::
          Events.made_ft_opponent.copy(min = 9.0, poss = -3) ::
          Events.drb_team.copy(min = 8.0, poss = -3) ::
          // 3T possesion - fouled and missed FT, ORB (2.*, 4.*)
          Events.made_team.copy(min = 7.0, poss = -3) ::
          Events.missed_ft_team.copy(min = 7.0, poss = -3) ::
          Events.orb_team.copy(min = 6.0, poss = -3) ::
          // 3T possesion - fouled and missed FT, DRB (2.*, 4.*)
          Events.made_team.copy(min = 5.0, poss = -3) ::
          Events.missed_ft_team.copy(min = 5.0, poss = -3) ::
          Events.drb_opponent.copy(min = 4.0, poss = -3) ::
          // Handle missed possesion (5.x)
          Events.missed_possession_opponent.copy(min = 3.0, poss = -4) ::
          Events.made_team.copy(min = 3.0, poss = -4) ::
          Events.made_opponent.copy(min = 1.0, poss = -5) :: //(5.x check compound clumps)
          Events.made_team.copy(min = 1.0, poss = -5) ::
          Nil

        List(
          game_1_events, game_2_events
        ).foreach { test_case_events =>
          val filtered_events = test_case_events.filter {
            case ev: Model.MiscGameEvent if ev.event_string.contains(error_event_string) => false
            case _ => true
          }
          TestUtils.inside(calculate_possessions(filtered_events)) {
            case res =>
              val expected = test_case_events.map {
                case ev: Model.MiscGameEvent => ev.with_poss(-ev.poss)
                case ev => ev
              }
              if (res != expected) {
                print_useful_list_diff(res, expected)
              }
              res ==> expected
          }
        }
      }
    }
  }
  /** (Useful debug if assertion below fails) */
  private def print_useful_list_diff(l1: List[_], l2: List[_]): Unit = {
    println("+++++++++++++++++++")
    println(
      l1.zip(l2).map { t2 =>
        val are_eq = if (t2._1 == t2._2) "T" else "F"
        s"[$are_eq]: [${t2._1}] V [${t2._2}]"
      }.mkString("\n")
    )
    println("-------------------")
  }
}
