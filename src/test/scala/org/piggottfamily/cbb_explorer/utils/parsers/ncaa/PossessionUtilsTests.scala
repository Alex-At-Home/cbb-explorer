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
        val steal_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "05:10,55-68,MASON III,FRANK Steal")

        val orb_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,Darryl Morsell, rebound offensive")
        val drb_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "10:00,51-60,Darryl Morsell, rebound defensive")
        val tech_opponent = Model.OtherOpponentEvent(0.0, gs, 0, "06:43:00,55-79,Bruno Fernando, foul technical classa;2freethrow")
      }

      "concurrent_event_handler" - {
        // (test check_for_concurrent_event and rearrange_concurrent_event)

        val test_events @ (
          ev1 :: ev2 :: ev3 :: ev4 :: ev_gb :: ev5 :: ev6 :: ev7
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


      "calculate_possessions" - {

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
