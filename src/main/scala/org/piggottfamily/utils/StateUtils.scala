package org.piggottfamily.utils

/** A collection of utilities for performing foldLeft and scanLeft operations
    to model state transition like activities
*/
object StateUtils {

  /** The various case classes and traits that model the state transitiin process */
  object StateTypes {
    case object NestedStashException extends Exception("Cannot stash during an unstash operation")

    trait StateContext[S, A, B] {
      /** The current set of events is from an unstash (can't stash if so) */
      def in_unstash: Boolean
      /** The current stash */
      def reverse_stash: List[A]
      /** The current list of output events */
      def reverse_out: List[B]

      // Transition handlers:
      def noChange: StateTransition[S, A, B] = NoChange()
      def constantState(b: B): StateTransition[S, A, B] = ConstantState(b)
      def constantState(bs: List[B]): StateTransition[S, A, B] = ConstantStateMulti(bs)
      def stateChange(s: S): StateTransition[S, A, B] = StateChange(s)
      def stateChange(new_s: S, b: B): StateTransition[S, A, B] = AllChange(new_s, b)
      def stateChange(new_s: S, bs: List[B]): StateTransition[S, A, B] = AllChangeMulti(new_s, bs)
      def stashElement(a: A): StateTransition[S, A, B] = StashElement(a)
      def stashElement(new_s: S, a: A): StateTransition[S, A, B] = StashChange(new_s, a)
      def stashElements(as: List[A]): StateTransition[S, A, B] = StashElements(as)
      def stashElements(new_s: S, as: List[A]): StateTransition[S, A, B] = StashChangeMulti(new_s, as)
      def unstashAll(reprocess_event: Boolean = false, remap: List[A] => List[A] = _.reverse): StateTransition[S, A, B] = UnstashAll(reprocess_event, remap)
      def discardStash(): StateTransition[S, A, B] = unstashAll(remap = _ => Nil)
    }

    // In
    sealed trait StateEvent[S, A, B]
    object StateEvent {
      case class Next[S, A ,B](ctx: StateContext[S, A, B], s: S, a: A) extends StateEvent[S, A, B]
      case class Complete[S, A, B](ctx: StateContext[S, A, B], s: S) extends StateEvent[S, A, B]
    }

    sealed trait StateEvent2[S, A, B]
    object StateEvent2 {
      case class First[S, A, B](ctx: StateContext[S, A, B], s: S, a: A) extends StateEvent2[S, A, B]
      /** Note prev_a might have been stashed, ie not processed */
      case class Next[S, A, B](ctx: StateContext[S, A, B], s: S, prev_a: A, a: A) extends StateEvent2[S, A, B]
      /** Note prev_a might have been stashed, ie not processed */
      case class Complete[S, A, B](ctx: StateContext[S, A, B], s: S, prev_a: Option[A]) extends StateEvent2[S, A, B]
    }

    // Out
    sealed trait StateTransition[S, A, B]
    case class NoChange[S, A, B]() extends StateTransition[S, A, B]
    case class StateChange[S, A, B](s: S) extends StateTransition[S, A, B]
    case class ConstantState[S, A, B](b: B) extends StateTransition[S, A, B]
    /** Note bs should be in "reverse order" wrt the input, eg last :: last_but_one :: ... :: first :: Nil */
    case class ConstantStateMulti[S, A, B](bs: List[B]) extends StateTransition[S, A, B]
    case class AllChange[S, A, B](s: S, b: B) extends StateTransition[S, A, B]
    /** Note bs should be in "reverse order" wrt the input, eg last :: last_but_one :: ... :: first :: Nil */
    case class AllChangeMulti[S, A, B](s: S, bs: List[B]) extends StateTransition[S, A, B]
    case class StashElement[S, A, B](a: A) extends StateTransition[S, A, B]
    /** Note bs should be in "reverse order" wrt the input, eg last :: last_but_one :: ... :: first :: Nil */
    case class StashElements[S, A, B](as: List[A]) extends StateTransition[S, A, B]
    case class StashChange[S, A, B](s: S, a: A) extends StateTransition[S, A, B]
    /** Note bs should be in "reverse order" wrt the input, eg last :: last_but_one :: ... :: first :: Nil */
    case class StashChangeMulti[S, A, B](s: S, as: List[A]) extends StateTransition[S, A, B]
    case class UnstashAll[S, A, B](reprocess_event: Boolean = false, remap: List[A] => List[A] = UnstashAll.defaultOrdering[A](_)) extends StateTransition[S, A, B]
    object UnstashAll {
      def defaultOrdering[A](in: List[A]): List[A] = in.reverse
      def discard[S, A, B]: UnstashAll[S, A, B] = UnstashAll(remap = _ => Nil)
    }

    // Final
    class FoldStateComplete[S, B](val s: S, val reverse_bs: List[B]) {
      /** The "correctly" ordered version of the list. Recalculated every time */
      def bs: List[B] = reverse_bs.reverse

      override def toString: String = s"FoldStateComplete($s, $reverse_bs)"
    }
    object FoldStateComplete {
      def unapply[S, B](fsc: FoldStateComplete[S, B]): Option[(S, List[B])] = Some(fsc.s, fsc.bs)
      object Raw {
        def unapply[S, B](fsc: FoldStateComplete[S, B]): Option[(S, List[B])] = Some(fsc.s, fsc.reverse_bs)
      }
      object State {
        def unapply[S](fsc: FoldStateComplete[S, _]): Option[S] = Some(fsc.s)
      }
    }
  }
  import StateTypes._

  /** A version of foldLeft for use in state transition like activities
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    */
  def foldLeft[S, A, B]
  (l: Seq[A], init: S, typeHint: Class[B]) //TODO Seq?
    (transition: StateEvent[S, A, B] => StateTransition[S, A, B])
  : FoldStateComplete[S, B] =
  {
    val init_temp_state = TempState[S, A, B](init)
    val phase1 = l.foldLeft(init_temp_state) { (acc, v) =>
      handle_unstash_command(
        Some(v), transition,
        handle_result(
          acc, transition(StateEvent.Next(acc, acc.s, v))
        )
      )
    }
    val phase2 = handle_unstash_command(
      None, transition,
      handle_result(
        phase1, transition(StateEvent.Complete(phase1, phase1.s))
      )
    )
    new FoldStateComplete(phase2.s, phase2.bs)
  }

  /** A version of foldLeft for use in state transition like activities
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    */
  def foldLeft[S, A]
  (l: Seq[A], init: S) //TODO Seq?
    (transition: StateEvent[S, A, A] => StateTransition[S, A, A])
  : FoldStateComplete[S, A] =
  {
    val init_temp_state = TempState[S, A, A](init)
    val phase1 = l.foldLeft(init_temp_state) { (acc, v) =>
      handle_unstash_command(
        Some(v), transition,
        handle_result(
          acc, transition(StateEvent.Next(acc, acc.s, v))
        )
      )
    }
    val phase2 = handle_unstash_command(
      None, transition,
      handle_result(
        phase1, transition(StateEvent.Complete(phase1, phase1.s))
      )
    )
    new FoldStateComplete(phase2.s, phase2.bs)
  }
  /** A version of foldLeft for use in state transition like activities
    * where the state transition is a function of the current and previous elements
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    * TODO seq?
    */
  def foldLeft2[S, A, B]
  (l: Seq[A], init: S, typeHint: Class[B])
    (transition: StateEvent2[S, A, B] => StateTransition[S, A, B])
  : FoldStateComplete[S, B] =
  {
    val init_temp_state = TempState[S, A, B](init)
    val phase1 = l.foldLeft(init_temp_state) {
      case (acc @ TempState(_, _, None, _, _), v) =>
        handle_unstash_command2_and_prev(
          None, Some(v), transition,
          handle_result(acc, transition(StateEvent2.First(acc, acc.s, v)))
        )
      case (acc @ TempState(_, _, Some(prev_v), _, _), v) =>
        handle_unstash_command2_and_prev(
          Some(prev_v), Some(v), transition,
          handle_result(acc, transition(StateEvent2.Next(acc, acc.s, prev_v, v)))
        )
    }
    val phase2 = handle_unstash_command2_and_prev(
      phase1.prev, None,  transition,
      handle_result(phase1, transition(StateEvent2.Complete(phase1, phase1.s, phase1.prev)))
    )
    new FoldStateComplete(phase2.s, phase2.bs)
  }

  /** A version of foldLeft for use in state transition like activities
    * where the state transition is a function of the current and previous elements
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    * TODO seq?
    */
  def foldLeft2[S, A]
  (l: Seq[A], init: S)
    (transition: StateEvent2[S, A, A] => StateTransition[S, A, A])
  : FoldStateComplete[S, A] =
  {
    val init_temp_state = TempState[S, A, A](init)
    val phase1 = l.foldLeft(init_temp_state) {
      case (acc @ TempState(_, _, None, _, _), v) =>
        handle_unstash_command2_and_prev(
          None, Some(v), transition,
          handle_result(acc, transition(StateEvent2.First(acc, acc.s, v)))
        )
      case (acc @ TempState(_, _, Some(prev_v), _, _), v) =>
        handle_unstash_command2_and_prev(
          Some(prev_v), Some(v), transition,
          handle_result(acc, transition(StateEvent2.Next(acc, acc.s, prev_v, v)))
        )
    }
    val phase2 = handle_unstash_command2_and_prev(
      phase1.prev, None,  transition,
      handle_result(phase1, transition(StateEvent2.Complete(phase1, phase1.s, phase1.prev)))
    )
    new FoldStateComplete(phase2.s, phase2.bs)
  }

  // Internals

  /** Translates between the StateUtils.foldLeft and the actual foldLeft models */
  private case class TempState[S, A, B](
    s: S, bs: List[B] = Nil,
    prev: Option[A] = None,
    stash: List[A] = Nil, unstash: Option[UnstashAll[S, A, B]] = None
  ) extends StateContext[S, A, B] {
    def reverse_stash: List[A] = stash
    def in_unstash: Boolean = unstash.nonEmpty
    def reverse_out: List[B] = bs
  }

  /** Translates between the StateUtils.foldLeft and the actual foldLeft responses */
  private def handle_result[S, A, B]
  (acc: TempState[S, A, B], res: StateTransition[S, A, B]): TempState[S, A, B] = res match
  {
    case NoChange() => acc
    case StateChange(new_s) => acc.copy(s = new_s)
    case ConstantState(b) => acc.copy(bs = b :: acc.bs)
    case ConstantStateMulti(bs) => acc.copy(bs = bs ++ acc.bs)
    case AllChange(new_s, b) => acc.copy(s = new_s, bs = b :: acc.bs)
    case AllChangeMulti(new_s, bs) => acc.copy(s = new_s, bs = bs ++ acc.bs)
    case StashElement(a) => acc.copy(stash = a :: acc.stash)
    case StashElements(as) => acc.copy(stash = as ++ acc.stash)
    case StashChange(new_s, a) => acc.copy(s = new_s, stash = a :: acc.stash)
    case StashChangeMulti(new_s, as) => acc.copy(s = new_s, stash = as ++ acc.stash)

    case unstash_command: UnstashAll[S, A, B] => acc.copy(unstash = Some(unstash_command))
  }

  /** Additional logic for unstashing */
  private def handle_unstash_command[S, A, B]
  (
    maybe_v: Option[A],
    transition: StateEvent[S, A, B] => StateTransition[S, A, B],
    new_acc: TempState[S, A, B]
  ): TempState[S, A, B] = new_acc match
  {
    case TempState(_, _, _, stash, Some(UnstashAll(reprocess_event, remap))) =>
      val list = remap(stash)
      val interm = list.foldLeft(new_acc) { (acc, v) => //(only one level of stashing allowed)
        val transition_result = transition(StateEvent.Next(acc, acc.s, v)) match {
          case _: StashElement[_, _, _] | _: StashElements[_, _, _] |
               _: StashChange[_, _, _] | _: StashChangeMulti[_, _, _] | _: UnstashAll[_, _, _] =>
            throw NestedStashException
          case res => res
        }
        handle_result(acc, transition_result)
      }
      val complete = if (reprocess_event) {
        handle_result(interm, transition(
          maybe_v.map(v => StateEvent.Next(interm, interm.s, v))
            .getOrElse(StateEvent.Complete(interm, interm.s))
        ))
      } else interm
      complete.copy(unstash = None)

    case new_s => new_s
  }

  /** Additional logic for unstashing */
  private def handle_unstash_command2_and_prev[S, A, B]
  (
    maybe_prev_v: Option[A], maybe_v: Option[A],
    transition: StateEvent2[S, A, B] => StateTransition[S, A, B],
    new_acc: TempState[S, A, B]
  ) : TempState[S, A, B] = new_acc match
  {
    case new_s @ TempState(s, _, _, stash, Some(UnstashAll(reprocess_event, remap))) =>
      /** Builds the next event based on the overall context */
      def next_event
      (
        curr_acc: TempState[S, A, B],
        maybe_prev_v: Option[A], maybe_v: Option[A]
      ): StateEvent2[S, A, B] = (maybe_prev_v, maybe_v) match
      {
        case (None, Some(v)) => StateEvent2.First(curr_acc, curr_acc.s, v)
        case (Some(prev_v), Some(v)) => StateEvent2.Next(curr_acc, curr_acc.s, prev_v, v)
        case (Some(prev_v), None) => StateEvent2.Complete(curr_acc, curr_acc.s, Some(prev_v))
        case (None, None) => StateEvent2.Complete(curr_acc, curr_acc.s, None)
      }

      val list = remap(stash)
      val adjusted_new_acc = new_acc.copy(prev = if (reprocess_event) maybe_prev_v else maybe_v)
      val interm = list.foldLeft(adjusted_new_acc) { (acc, v) => //(only one level of stashing allowed)
        val transition_result =  transition(next_event(acc, acc.prev, Some(v))) match {
          case _: StashElement[_, _, _] | _: StashElements[_, _, _] |
               _: StashChange[_, _, _] | _: StashChangeMulti[_, _, _] | _: UnstashAll[_, _, _] =>
            throw NestedStashException
          case res => res
        }
        handle_result(acc, transition_result)
      }
      val complete = if (reprocess_event) {
        handle_result(interm, transition(
          next_event(interm, interm.prev, maybe_v)
        ))
      } else interm
      complete.copy(prev = maybe_v, unstash = None)

    case new_s => new_s.copy(prev = maybe_v)
  }

  //TODO scanLeft, which returns the same "list" type as input together with an incremental view of the state

}
