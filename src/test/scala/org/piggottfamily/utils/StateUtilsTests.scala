package org.piggottfamily.utils

import utest._
import org.piggottfamily.cbb_explorer.utils.TestUtils

object StateUtilsTests extends TestSuite {
  import StateUtils.StateTypes._

  val tests = Tests {
    "StateUtils" - {

      val input = List(1, 2, 3, 4, 5)

      "FoldStateComplete" - {
        (new FoldStateComplete(1, List(2, 3))).toString ==> "FoldStateComplete.Raw(1, List(2, 3))"
      }

      "foldLeft(l, s)" - {
        case class State(sum: Int)
        val res = StateUtils.foldLeft(input, State(0)) {
          case StateEvent.Next(ctx, s, event) =>
            ctx.stateChange(State(s.sum + event), event)
          case StateEvent.Complete(ctx, s) =>
            ctx.stateChange(s.copy(sum = s.sum + 100))
        }
        TestUtils.inside(res) {
          case FoldStateComplete(
            State(115), List(1, 2, 3, 4, 5)
          ) =>
        }
      }
      "foldLeft(l, s, typeHint)" - {
        case class State(l: List[String])

        // Check state and mapped output

        val res = StateUtils.foldLeft(input, State(Nil), classOf[String]) {
          case StateEvent.Next(ctx, _, event) if 1 == event =>
            ctx.noChange
          case StateEvent.Next(ctx, state, event) if 2 == event =>
            ctx.stateChange(state.copy(l = event.toString :: state.l))
          case StateEvent.Next(ctx, state, event) if 3 == event =>
            ctx.constantState(List("3b", "3a"))
          case StateEvent.Next(ctx, state, event) if 4 == event =>
            ctx.stateChange(state.copy(l = event.toString :: state.l), "4")
          case StateEvent.Next(ctx, state, event) if 5 == event =>
            ctx.stateChange(state.copy(l = event.toString :: state.l), List("5b", "5a"))

          case StateEvent.Complete(ctx, _) =>
            ctx.constantState("final")
        }
        TestUtils.inside(res) {
          case FoldStateComplete(
            State(List("5", "4", "2")), List("3a", "3b", "4", "5a", "5b", "final")
          ) =>
        }
        TestUtils.inside(res) {
          case FoldStateComplete.Raw(
            State(List("5", "4", "2")), List("final", "5b", "5a", "4", "3b", "3a")
          ) =>
        }
        TestUtils.inside(res) {
          case FoldStateComplete.State(State(List("5", "4", "2"))) =>
        }

        // Empty list:
        TestUtils.inside(StateUtils.foldLeft(List[Int](), State(Nil), classOf[String]) {
          case StateEvent.Next(ctx, _, _) =>
            ctx.constantState("unexpected_next")
          case StateEvent.Complete(ctx, _) =>
            ctx.stateChange(State(List("complete")))
        }) {
          case FoldStateComplete(State(List("complete")), Nil) =>
        }
      }
      "foldLeft(l, s) - stash" - {
        // Discard all
        TestUtils.inside(StateUtils.foldLeft(input, 0, classOf[String]) {
          case StateEvent.Next(ctx, _, _) =>
            ctx.stashElement(0)
          case StateEvent.Complete(ctx, _) =>
            ctx.discardStash()
        }) {
          case FoldStateComplete(0, Nil) =>
        }
        // Stash and then ignore
        TestUtils.inside(StateUtils.foldLeft(input, 0, classOf[String]) {
          case StateEvent.Next(ctx, _, _) =>
            ctx.stashElement(0)
          case StateEvent.Complete(ctx, _) =>
            ctx.stateChange(100)
        }) {
          case FoldStateComplete(100, Nil) =>
        }

        //Simple stash case - exhaustive check of all ctx commands together with "foldLeft2(l, s) - stash"

        case class State(l: List[String])

        val res = StateUtils.foldLeft(input, State(Nil), classOf[String]) {
          case StateEvent.Next(ctx, _, event) if 1 == event =>
            ctx.noChange

          case StateEvent.Next(ctx, state, event) if event <= 2 && ctx.unstashing =>
            ctx.constantState(event.toString)
          case StateEvent.Next(ctx, state, event) if 2 == event =>
            ctx.stashElements(List(2, -2))

          case StateEvent.Next(ctx, state, event) if 3 == event && ctx.has_stash =>
            ctx.unstashAll(reprocess_event = false, remap = (as: List[Int]) => as)
          case StateEvent.Next(ctx, state, event) if 3 == event => //(never reached)
            ctx.constantState(List("3b", "3a"))

          case StateEvent.Next(ctx, state, event) if 4 == event && ctx.unstashing =>
            ctx.constantState(event.toString)
          case StateEvent.Next(ctx, state, event) if 4 == event =>
            ctx.stashElement(state.copy(l = event.toString :: state.l), 4)

          case StateEvent.Next(ctx, state, event) if 5 == event =>
            ctx.stateChange(state.copy(l = event.toString :: state.l), List("5b", "5a"))

          case StateEvent.Complete(ctx, state) if ctx.has_stash =>
            ctx.unstashAll(state.copy(l = "complete_unstash" :: state.l), reprocess_event = true)
          case StateEvent.Complete(ctx, _) =>
            ctx.constantState("final")
        }
        TestUtils.inside(res) {
          case FoldStateComplete(
            State(List("complete_unstash", "5", "4")), List("2", "-2", "5a", "5b", "4", "final")
          ) =>
        }
      }

      "foldLeft2(l, s)" - {
        case class State(sum: Int)
        val res = StateUtils.foldLeft2(input, State(0)) {
          case StateEvent2.First(ctx, s, event) =>
            ctx.stateChange(State(s.sum + 100), event)
          case StateEvent2.Next(ctx, s, prev_event, event) =>
            ctx.stateChange(State(s.sum + prev_event + event), List(event, prev_event))
            // 1+2=3, 2+3=5, 3+4=7, 4+5=9, total 24
          case StateEvent2.Complete(ctx, s, prev_event) =>
            ctx.stateChange(s.copy(sum = s.sum + 1000), prev_event.getOrElse(-1))
        }
        TestUtils.inside(res) {
          case FoldStateComplete(
            State(1124), List(1, 1, 2, 2, 3, 3, 4, 4, 5, 5)
          ) =>
        }
      }
      "foldLeft2(l, s, typeHint)" - {
        // Empty list:
        TestUtils.inside(StateUtils.foldLeft2(List[Int](), "", classOf[String]) {
          case StateEvent2.First(ctx, _, _) =>
            ctx.constantState("unexpected_first")
          case StateEvent2.Next(ctx, _, _, _) =>
            ctx.constantState("unexpected_next")
          case StateEvent2.Complete(ctx, _, prev) =>
            ctx.stateChange(s"complete_$prev", s"complete_$prev")
        }) {
          case FoldStateComplete("complete_None", List("complete_None")) =>
        }
      }

      "foldLeft2(l, s) - stash" - {
        case class State(sum: Int)
        val res = StateUtils.foldLeft2(input, State(0)) {
          case StateEvent2.First(ctx, s, event) => //(1)
            ctx.stateChange(State(s.sum + 100), event)

          case StateEvent2.Next(ctx, s, prev_event, event) if ctx.unstashing => //(5,2), (2,3) = 12
            ctx.stateChange(State(s.sum + prev_event + event), List(event, prev_event))

          case StateEvent2.Next(ctx, s, prev_event, event) if event == 2 =>
            ctx.stashElement(State(s.sum + 10000), event)
          case StateEvent2.Next(ctx, s, prev_event, event) if event <= 3 =>
            ctx.stashElement(event)

          case StateEvent2.Next(ctx, s, prev_event, event) => //(3,4), (4,5) == 16
            ctx.stateChange(State(s.sum + prev_event + event), List(event, prev_event))

          case StateEvent2.Complete(ctx, s, prev_event) if ctx.has_stash =>
            ctx.unstashAll(reprocess_event = true)
          case StateEvent2.Complete(ctx, s, prev_event) if ctx.reprocessed => //(prev_element is 3 here because of the unstash)
            ctx.stateChange(s.copy(sum = s.sum + 1000), prev_event.getOrElse(-1))
          case StateEvent2.Complete(ctx, s, prev_event) => //shouldn't be here
            ctx.stateChange(State(-100)) //(ie fail test with clear signature)
        }
        TestUtils.inside(res) {
          case FoldStateComplete(
            State(11128), List(1, 3, 4, 4, 5, 5, 2, 2, 3, 3)
          ) =>
        }

        // Want to make sure I get the right next/prev more explicity
        case class StashState(l: List[String] = Nil)
        List(true, false).foreach { reproc =>
          val res2 = StateUtils.foldLeft2(input.take(4).map(_*10), StashState()) {
            case StateEvent2.First(ctx, s, event) => //(1)
              ctx.stateChange(s.copy(s"SE2F.$event" :: s.l))
            case StateEvent2.Next(ctx, s, prev_event, event) if event == 20 && !ctx.unstashing =>
              ctx.stashElements(List(15, 10))
            case StateEvent2.Next(ctx, s, prev_event, event) if event == 30 && !ctx.unstashing =>
              ctx.stashElement(20)
            case StateEvent2.Next(ctx, s, prev_event, event) if event == 40 && !ctx.reprocessed =>
              ctx.unstashAll(s.copy(s"SE2N.$prev_event.$event" :: s.l), reprocess_event = reproc)
            case StateEvent2.Next(ctx, s, prev_event, event) =>
              ctx.stateChange(s.copy(s"SE2N.$prev_event.$event" :: s.l))
            case StateEvent2.Complete(ctx, s, prev_event) => //(1)
              ctx.stateChange(s.copy((s"SE2C.$prev_event" :: s.l).reverse))
          }
          TestUtils.inside(res2) {
            case FoldStateComplete.State(
              StashState(
                "SE2F.10" :: "SE2N.30.40" :: "SE2N.10.15" :: "SE2N.15.20" ::  "SE2N.20.30" :: "SE2N.30.40" :: "SE2C.40" :: Nil
              )
            ) if reproc =>
              //Currently this is: StashState(List(SE2F.10, SE2N.30.40, SE2N.30.10, SE2N.10.15, SE2N.15.20, SE2N.20.40, SE2C.Some(40)))

            case FoldStateComplete.State(
              StashState(
                "SE2F.10" :: "SE2N.10.15" :: "SE2N.15.20" ::  "SE2N.20.30" :: "SE2C.30" :: Nil
              )
            ) if !reproc =>
          }
        }
      }
    }
  }
}
