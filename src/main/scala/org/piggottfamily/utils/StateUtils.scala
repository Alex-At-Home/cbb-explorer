package org.piggottfamily.utils

/** A collection of utilities for performing foldLeft and scanLeft operations
    to model state transition like activities
*/
object StateUtils {

  /** The various case classes and traits that model the state transitiin process */
  object StateTypes {
    case object NestedStashException extends Exception("Cannot stash during an unstash operation")

    trait StateContext[S, A, B] {
      /** The current list of output elements */
      def reverseOut: List[B]

      /** The current list of clumped (unprocessed) elements */
      def reverseClump: List[A]

      // Transition handlers:
      def noChange: StateTransition[S, A, B] = NoChange()
      def constantState(b: B): StateTransition[S, A, B] = ConstantState(b)
      def constantState(bs: List[B]): StateTransition[S, A, B] = ConstantStateMulti(bs)
      def stateChange(s: S): StateTransition[S, A, B] = StateChange(s)
      def stateChange(new_s: S, b: B): StateTransition[S, A, B] = AllChange(new_s, b)
      def stateChange(new_s: S, bs: List[B]): StateTransition[S, A, B] = AllChangeMulti(new_s, bs)
    }

    // In
    sealed trait StateEvent[S, A, B]
    object StateEvent {
      case class Next[S, A ,B](ctx: StateContext[S, A, B], s: S, a: A) extends StateEvent[S, A, B]
      case class Complete[S, A, B](ctx: StateContext[S, A, B], s: S) extends StateEvent[S, A, B]
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

    // Final
    class FoldStateComplete[S, B](val s: S, val reverse_bs: List[B]) {
      /** The "correctly" ordered version of the list. Recalculated every time */
      def bs: List[B] = reverse_bs.reverse

      override def toString: String = s"FoldStateComplete.Raw($s, $reverse_bs)"
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

    /**
     * Allows the incoming list to be clumped and rearranged (etc) within each clump
     * We return true if the element is possibly part of a clump
     * (ie should always be true for first element)
     */
    case class Clumper[S, SS, A](
      init_clump_state: SS,
      expand_clump: Clumper.Event[S, SS, A] => (SS, Boolean),
      remap_clump: (S, SS, List[A]) => List[A]
    )
    object Clumper {
      /**
       *  The input to the clumper
       */
      case class Event[S, SS, A](state: S, clump_state: SS, reverse_clump: List[A], candidate: A)

      def empty[S, A]: Clumper[S, _, A] = Clumper(
        (), (_: Clumper.Event[S, Unit, A]) => ((), false), (_: S, _: Unit, as: List[A]) => as.reverse
      )
    }

  }
  import StateTypes._

  /** A version of foldLeft for use in state transition like activities
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    */
  def foldLeft[S, SS, A, B]
    (l: Seq[A], init: S, typeHint: Class[B], clumper: Clumper[S, SS, A])
    (transition: StateEvent[S, A, B] => StateTransition[S, A, B])
  : FoldStateComplete[S, B] =
  {
    val init_temp_state = TempState[S, SS, A, B](init, clumper.init_clump_state)

    def handle_clump_state(
      clump_state: SS, acc: TempState[S, SS, A, B]
    ): TempState[S, SS, A, B] = {
      clumper.remap_clump(acc.s, clump_state, acc.reverse_clump).foldLeft(acc) { (inner_acc, v) =>
        handle_result(inner_acc, transition(StateEvent.Next(inner_acc, inner_acc.s, v)))
      }
    }

    val phase1 = l.foldLeft(init_temp_state) { (acc, v) =>
      val (clump_state, expand_clump) = clumper.expand_clump(
        Clumper.Event(acc.s, acc.clump_state, acc.reverse_clump, v)
      )
      if (expand_clump) {
        acc.copy(clump_state = clump_state, reverse_clump = v :: acc.reverse_clump)
      } else { // first process the clump and then append the new element as a new clump
        handle_clump_state(clump_state, acc).copy(
          clump_state = clump_state,
          reverse_clump = v :: Nil
        )
      }
    }
    val phase2 = handle_clump_state(phase1.clump_state, phase1) //(flush pending clump)
    val phase3 = handle_result(phase2, transition(StateEvent.Complete(phase2, phase2.s)))
    new FoldStateComplete(phase3.s, phase3.bs)
  }

  /** A version of foldLeft for use in state transition like activities
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    */
  def foldLeft[S, A, B]
    (l: Seq[A], init: S, typeHint: Class[B])
    (transition: StateEvent[S, A, B] => StateTransition[S, A, B])
  : FoldStateComplete[S, B] =
    foldLeft(l, init, typeHint, Clumper.empty[S, A])(transition)


  /** A version of foldLeft for use in state transition like activities
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    */
  def foldLeft[S, SS, A]
    (l: Seq[A], init: S, clumper: Clumper[S, SS, A])
    (transition: StateEvent[S, A, A] => StateTransition[S, A, A])
    (implicit ctag: reflect.ClassTag[A])
  : FoldStateComplete[S, A] =
    foldLeft(l, init, ctag.runtimeClass.asInstanceOf[Class[A]], clumper)(transition)

  /** A version of foldLeft for use in state transition like activities
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    */
  def foldLeft[S, A]
    (l: Seq[A], init: S)
    (transition: StateEvent[S, A, A] => StateTransition[S, A, A])
    (implicit ctag: reflect.ClassTag[A])
  : FoldStateComplete[S, A] =
    foldLeft(l, init, ctag.runtimeClass.asInstanceOf[Class[A]], Clumper.empty[S, A])(transition)

  // Internals

  /** Translates between the StateUtils.foldLeft and the actual foldLeft models */
  private case class TempState[S, SS, A, B](
    s: S, clump_state: SS,
    bs: List[B] = Nil, reverse_clump: List[A] = Nil
  ) extends StateContext[S, A, B] {
    def reverseOut: List[B] = bs
    def reverseClump: List[A] = reverse_clump
  }

  /** Translates between the StateUtils.foldLeft and the actual foldLeft responses */
  private def handle_result[S, SS, A, B]
    (acc: TempState[S, SS, A, B], res: StateTransition[S, A, B]): TempState[S, SS, A, B] = res match
  {
    case NoChange() => acc
    case StateChange(new_s) => acc.copy(s = new_s)
    case ConstantState(b) => acc.copy(bs = b :: acc.bs)
    case ConstantStateMulti(bs) => acc.copy(bs = bs ++ acc.bs)
    case AllChange(new_s, b) => acc.copy(s = new_s, bs = b :: acc.bs)
    case AllChangeMulti(new_s, bs) => acc.copy(s = new_s, bs = bs ++ acc.bs)
  }

  //TODO scanLeft, which returns the same "list" type as input together with an incremental view of the state

}
