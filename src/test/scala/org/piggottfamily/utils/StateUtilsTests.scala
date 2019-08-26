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
      "foldLeft(l, s) - clump" - {

        val clumped_list = List("a1", "a2", "b1", "b2", "c3", "c1", "c2", "d2", "d1")
        case class State(l: List[String], ignore: Boolean)

        def clump_elements(ev: Clumper.Event[State, String, String]): (String, Boolean) = ev match {
          case Clumper.Event(State(_, true), _, _, _) =>
            ("X", false)
          case Clumper.Event(_, _, Nil, a) =>
            (a.take(1), true)
          case Clumper.Event(_, ss, _, a) if a.take(1) == ss =>
            (ss, true)
          case Clumper.Event(_, ss, _, a) =>
            (a.take(1), false)
        }
        def remap_elements(s: State, ss: String, as: List[String]): List[String] = {
          if (ss == "a") as.reverse
          else if (ss == "b") as
          else as.sorted
        }
        var clumper = Clumper(
          "Y", clump_elements _, remap_elements _
        )
        TestUtils.inside(StateUtils.foldLeft(clumped_list, State(Nil, false), clumper) {
          case StateEvent.Next(ctx, s, a) if a == "c3" => //(hence d will end up as 2 clumps)
            ctx.stateChange(s.copy(l = a :: s.l, ignore = true), a)
          case StateEvent.Next(ctx, s, a) =>
            ctx.stateChange(s.copy(l = a :: s.l), a)
          case StateEvent.Complete(ctx, s) =>
            ctx.stateChange(State(s.l.reverse, false))
        }) {
          case FoldStateComplete(
            State(l1, false),
            l2
          ) =>
            val expected_list = List("a2", "a1", "b1", "b2", "c1", "c2", "c3", "d2", "d1")
            l2 ==> expected_list
            l1 ==> expected_list
        }
      }
    }
  }
}
