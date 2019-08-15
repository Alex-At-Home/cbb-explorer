package org.piggottfamily.utils

import utest._
import org.piggottfamily.cbb_explorer.utils.TestUtils

object StateUtilsTests extends TestSuite {
  import StateUtils.StateTypes._

  val tests = Tests {
    "StateUtils" - {

      val input = List(1, 2, 3, 4, 5)

      "FoldStateComplete" - {
        (new FoldStateComplete(1, List(2, 3))).toString ==> "FoldStateComplete(1, List(2, 3))"
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
      //TODO foldLeft stash

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
      //TODO foldLeft2 stash
    }
  }
}
