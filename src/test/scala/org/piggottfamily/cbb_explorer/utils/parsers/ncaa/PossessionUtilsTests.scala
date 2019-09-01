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

      "concurrent_event_handler" - {
        // (test check_for_concurrent_event and rearrange_concurrent_event)

        val test_events @ (ev1 :: ev2 :: ev3 :: ev4 :: ev_gb :: ev5 :: ev6 :: ev7:: Nil) =
          Model.OtherTeamEvent(0.4, Game.Score(0, 0), 1, "ev-1") ::
          Model.OtherOpponentEvent(0.5, Game.Score(0, 0), 1, "ev-2") ::
          Model.OtherTeamEvent(0.9, Game.Score(0, 0), 1, "ev-3") ::
          Model.OtherOpponentEvent(0.9, Game.Score(0, 0), 1, "ev-4") ::
          Model.GameBreakEvent(0.9) ::
          Model.OtherTeamEvent(0.9, Game.Score(0, 0), 1, "ev-5") ::
          Model.OtherOpponentEvent(0.9, Game.Score(0, 0), 1, "ev-6") ::
          Model.OtherTeamEvent(1.0, Game.Score(0, 0), 1, "ev-7") ::
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

      val gs = Game.Score(0, 0)

      /** A handy compilation of events */
      object Events {
        val game_break = Model.GameBreakEvent(0.9)

        val turnover_team = Model.OtherTeamEvent(0.0, gs, 0, "08:44:00,20-23,Bruno Fernando, turnover badpass")
        val steal_team = Model.OtherTeamEvent(0.0, gs, 1, "05:10,55-68,MASON III,FRANK Steal")
        val made_team = Model.OtherTeamEvent(0.0, gs, 1, "10:00,51-60,SMITH,JALEN made Three Point Jumper")
        val made_ft_team = Model.OtherTeamEvent(0.0, gs, 1, "05:10,55-68,Kevin Anderson, freethrow 2of2 made")
        val missed_ft_team = Model.OtherTeamEvent(0.0, gs, 1, "10:00,51-60,DREAD,MYLES missed Free Throw")
        val orb_team = Model.OtherTeamEvent(0.0, gs, 0, "10:00,51-60,Darryl Morsell, rebound offensive")

        val miss_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "02:28:00,27-38,Eric Ayala, 3pt jumpshot 2ndchance missed")
        val foul_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,MYKHAILIUK,SVI Commits Foul")
        val made_ft_opponent = Model.OtherOpponentEvent(0.0, gs, 1, "05:10,55-68,Kevin Anderson, freethrow 2of2 made")
        val missed_ft_opponent = Model.OtherOpponentEvent(0.0, gs, 1, "10:00,51-60,DREAD,MYLES missed Free Throw")

        val drb_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,Darryl Morsell, rebound defensive")
        val tech_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "06:43:00,55-79,Bruno Fernando, foul technical classa;2freethrow")

        val unknown_team = Model.OtherTeamEvent(0.0, gs, 0, "UNKNOWN_EVENT")
      }

      "first_possession_status" - {
        val test_event_pairs =
          (Events.turnover_team -> Some(Direction.Team)) ::
          (Events.steal_team -> Some(Direction.Opponent)) ::
          (Events.miss_opponent -> Some(Direction.Opponent)) ::
          (Events.foul_opponent -> Some(Direction.Team)) ::
          (Events.unknown_team -> None) ::
          Nil

        TestUtils.inside(
          test_event_pairs.zipWithIndex.map(_._2).map { test_index =>
            first_possession_status(test_event_pairs.drop(test_index).map(_._1))
          }
        ) {
          case l =>
            l ==> test_event_pairs.map(_._2)
        }
      }

      "clump_possession_status" - {

        val test_event_pairs =
          (Events.game_break -> PossessionArrowSwitch) ::
          (Events.drb_opponent -> PossessionEnd(last_clump = true)) ::
          (Events.turnover_team -> PossessionEnd()) ::
          (Events.tech_opponent -> PossessionContinues) ::
          (Events.made_team -> PossessionContinues) :: //(cos of the missed and one next)
          (Events.missed_ft_team -> PossessionContinues) :: //(_not_ unclear because of shot makes ahead)
          (Events.made_team -> PossessionEnd()) ::
          (Events.made_ft_team -> PossessionEnd()) :: //(no missed FTs follow)
          (Events.made_team -> PossessionEnd()) :: //(no FTs after)
          (Events.unknown_team -> PossessionContinues) :: //(no FTs after)
          Nil

        val opponent_ft_pairs =
          (Events.made_ft_opponent -> PossessionUnclear) ::
          (Events.missed_ft_opponent -> PossessionContinues) ::
          Nil

        val test_unclear_pairs_no_orb = //almost all events get overwritten by a poss end
          test_event_pairs(0) ::
          test_event_pairs(1) ::
          test_event_pairs.drop(2).map(t2 => (t2._1, PossessionEnd(last_clump = true)))

        val test_unclear_pairs_orb =
          test_event_pairs ++ List(
             (Events.orb_team -> PossessionContinues)
          )

        val events_that_error_team =
          (List(Events.steal_team) -> PossessionError) ::
          (List(Events.miss_opponent) -> PossessionError) ::
          Nil

        val events_that_error_oppo =
          (List(Events.made_team) -> PossessionError) ::
          (List(Events.turnover_team) -> PossessionError) ::
          Nil

        val team_state = PossState.init.copy(direction = Direction.Team)
        val oppo_state = PossState.init.copy(direction = Direction.Opponent)
        val poss_unclear_state = team_state.copy(status = PossState.Status.Unclear)

        sealed trait TestCase
        case class TestCase1[T](s: PossState, l: List[(Model.PlayByPlayEvent, T)]) extends TestCase
        case class TestCase2[T](s: PossState, l: List[(List[Model.PlayByPlayEvent], T)]) extends TestCase

        List(
          TestCase1(team_state, test_event_pairs),
          TestCase1(oppo_state, opponent_ft_pairs),
          TestCase1(poss_unclear_state, test_unclear_pairs_no_orb),
          TestCase1(poss_unclear_state, test_unclear_pairs_orb),
          TestCase2(team_state, events_that_error_team),
          TestCase2(oppo_state, events_that_error_oppo),
        ).foreach {
          case TestCase1(s, l) =>
            TestUtils.inside(
              l.zipWithIndex.map(_._2).map { test_index =>
                clump_possession_status(s, l.drop(test_index).map(_._1))
              }
            ) {
              case res =>
                res ==> l.map(_._2)
            }
            case TestCase2(s, ll) => ll.foreach { case (l, exp) =>
              TestUtils.inside(clump_possession_status(s, l)) {
                case `exp` =>
              }
            }
        }
      }

      "calculate_possessions" - {
        //TODO test cases

        // Test Cases 1.*: Check that correctly calculates first possession
        // Test Cases 2.*: Check that possession change is handled correctly across a game break
        // Test Cases 3.*: Check that rebounds are handled correctly
        // Test Cases 4.*: Check that after a possession error everything stops        
      }
    }
  }
}
