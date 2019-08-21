package org.piggottfamily.utils

/** A collection of utilities for performing foldLeft and scanLeft operations
    to model state transition like activities
*/
object StateUtils {

  /** The various case classes and traits that model the state transitiin process */
  object StateTypes {
    case object NestedStashException extends Exception("Cannot stash during an unstash operation")

    trait StateContext[S, A, B] {
      /** Whether we are the isReprocessed element at the end of an unstash */
      def isReprocessed: Boolean = unstashing && !hasStash
      /** Whether there is anything in the stash (convenience to avoid "reverseStash.nonEmpty") */
      def hasStash: Boolean = reverseStash.nonEmpty
      /** The current set of elements is from an unstash (can't stash if so, unless also "reprocessed") */
      def unstashing: Boolean
      /** The current stash */
      def reverseStash: List[A]
      /** The current list of output elements */
      def reverseOut: List[B]

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
      def unstashAll(reprocess_element: Boolean): StateTransition[S, A, B] = UnstashAll(reprocess_element)
      def unstashAll(reprocess_element: Boolean, remap: List[A] => List[A]): StateTransition[S, A, B] = UnstashAll(reprocess_element, remap)
      def unstashAll(new_s: S, reprocess_element: Boolean): StateTransition[S, A, B] = UnstashAllChange(new_s, reprocess_element)
      def unstashAll(new_s: S, reprocess_element: Boolean, remap: List[A] => List[A]): StateTransition[S, A, B] = UnstashAllChange(new_s, reprocess_element, remap)
      def discardStash(): StateTransition[S, A, B] = unstashAll(reprocess_element = false, remap = _ => Nil)
      def discardStash(new_s: S): StateTransition[S, A, B] = unstashAll(new_s, reprocess_element = false, remap = _ => Nil)
    }

    // In
    sealed trait StateEvent[S, A, B]
    object StateEvent {
      case class Next[S, A ,B](ctx: StateContext[S, A, B], s: S, a: A) extends StateEvent[S, A, B]
      case class Complete[S, A, B](ctx: StateContext[S, A, B], s: S) extends StateEvent[S, A, B]

      case class StashStart[S, A, B](s: S, unstash_element: StateEvent[S, A, B], ordered_stash: List[A]) extends StateEvent[S, A, B]
      case class StashEnd[S, A, B](s: S, unstash_element: StateEvent[S, A, B], ordered_stash: List[A]) extends StateEvent[S, A, B]
    }

    sealed trait StateEvent2[S, A, B]
    object StateEvent2 {
      case class First[S, A, B](ctx: StateContext[S, A, B], s: S, a: A) extends StateEvent2[S, A, B]
      /** Note prev_a might have been stashed, ie not processed */
      case class Next[S, A, B](ctx: StateContext[S, A, B], s: S, prev_a: A, a: A) extends StateEvent2[S, A, B]
      /** Note prev_a might have been stashed, ie not processed */
      case class Complete[S, A, B](ctx: StateContext[S, A, B], s: S, prev_a: Option[A]) extends StateEvent2[S, A, B]

      case class StashStart[S, A, B](s: S, a: Option[A], unstash_element: StateEvent2[S, A, B], ordered_stash: List[A]) extends StateEvent2[S, A, B]
      case class StashEnd[S, A, B](s: S, prev_a: Option[A], unstash_element: StateEvent2[S, A, B], ordered_stash: List[A]) extends StateEvent2[S, A, B]

      /** Shortcut for pulling out the element that will become prev next iteration */
      object EventsThatGeneratePrevs {
        def unapply[S, A, B](ev: StateEvent2[S, A, B]): Option[A] = ev match {
          case First(_, a) => Some(a)
          case Next(_, _, a) => Some(a)
          case _ => None
        }
      }
    }

    // Out
    sealed trait StateTransition[S, A, B]
    object StateTransition {
      /** Utility for spotting stash related commands (that can't be issued inside stashes) */
      object IsUnstashRelated {
        def unapply(t: StateTransition[_, _, _]): Option[StateTransition[_, _, _]] = t match {
          case _: StashElement[_, _, _] | _: StashElements[_, _, _] |
               _: StashChange[_, _, _] | _: StashChangeMulti[_, _, _] | _: UnstashAll[_, _, _] => Some(t)
          case  _ => None
        }
      }
    }
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
    case class UnstashAll[S, A, B](reprocess_element: Boolean = false, remap: List[A] => List[A] = UnstashAll.defaultOrdering[A](_)) extends StateTransition[S, A, B]
    object UnstashAll {
      def defaultOrdering[A](in: List[A]): List[A] = in.reverse
      def discard[S, A, B]: UnstashAll[S, A, B] = UnstashAll(remap = _ => Nil)
    }
    case class UnstashAllChange[S, A, B](new_s: S, reprocess_element: Boolean = false, remap: List[A] => List[A] = UnstashAll.defaultOrdering[A](_)) extends StateTransition[S, A, B]

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
  }
  import StateTypes._

  /** A version of foldLeft for use in state transition like activities
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    */
  def foldLeft[S, A, B]
    (l: Seq[A], init: S, typeHint: Class[B])
    (transition: StateEvent[S, A, B] => StateTransition[S, A, B])
  : FoldStateComplete[S, B] =
  {
    val init_temp_state = TempState[S, A, B](init)
    val phase1 = l.foldLeft(init_temp_state) { (acc, v) =>
      command_completion(
        Some(v),
        transition, StateEvent.Next(acc, acc.s, v),
        handle_result(acc, _)
      )
    }
    val phase2 = command_completion[S, A, B](
      None,
      transition, StateEvent.Complete(phase1, phase1.s),
      handle_result(phase1, _)
    )
    new FoldStateComplete(phase2.s, phase2.bs)
  }

  /** A version of foldLeft for use in state transition like activities
    * Note that you incur the cost of a reverse on the output list
    * TODO scaladoc
    */
  def foldLeft[S, A]
  (l: Seq[A], init: S)
    (transition: StateEvent[S, A, A] => StateTransition[S, A, A])
    (implicit ctag: reflect.ClassTag[A])
  : FoldStateComplete[S, A] =
    foldLeft(l, init, ctag.runtimeClass.asInstanceOf[Class[A]])(transition)

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
    // (NOTE: I tried rewriting this in terms of foldLeft but stash logic made this
    // more trouble than it was worth, though getting rid of "command_completion2"
    // would have been nice)
    val init_temp_state = TempState[S, A, B](init)
    val phase1 = l.foldLeft(init_temp_state) {
      case (acc @ TempState(_, _, None, _, _), v) =>
        command_completion2(
          None, Some(v),
          transition, StateEvent2.First(acc, acc.s, v),
          handle_result(acc, _)
        )
      case (acc @ TempState(_, _, Some(prev_v), _, _), v) =>
        command_completion2(
          Some(prev_v), Some(v),
          transition, StateEvent2.Next(acc, acc.s, prev_v, v),
          handle_result(acc, _)
        )
    }
    val phase2 = command_completion2[S, A, B](
      phase1.prev, None,
      transition, StateEvent2.Complete(phase1, phase1.s, phase1.prev),
      handle_result(phase1, _)
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
    (implicit ctag: reflect.ClassTag[A])
  : FoldStateComplete[S, A] =
    foldLeft2(l, init, ctag.runtimeClass.asInstanceOf[Class[A]])(transition)

  // Internals

  /** Translates between the StateUtils.foldLeft and the actual foldLeft models */
  private case class TempState[S, A, B](
    s: S, bs: List[B] = Nil,
    prev: Option[A] = None,
    stash: List[A] = Nil, unstash: Option[UnstashAll[S, A, B]] = None
  ) extends StateContext[S, A, B] {
    def reverseStash: List[A] = stash
    def unstashing: Boolean = unstash.nonEmpty
    def reverseOut: List[B] = bs
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
    case us_cmd: UnstashAllChange[S, A, B] =>
      acc.copy(s = us_cmd.new_s, unstash = Some(UnstashAll(us_cmd.reprocess_element, us_cmd.remap)))
  }

  /** Additional logic for unstashing */
  private def command_completion[S, A, B](
    maybe_v: Option[A],
    transition: StateEvent[S, A, B] => StateTransition[S, A, B],
    result: StateEvent[S, A, B],
    result_handler: StateTransition[S, A, B] => TempState[S, A, B]
  ): TempState[S, A, B] = (transition andThen result_handler)(result) match
  {
    case new_s @ TempState(_, _, _, stash, Some(UnstashAll(reprocess_element, remap))) =>
      val element_list = remap(stash)
      val event_fn_list: List[TempState[S, A, B] => StateEvent[S, A, B]] = (
        ((acc: TempState[S, A, B]) => StateEvent.StashStart(acc.s, result, element_list))
        ::
        element_list.map(v => ((acc: TempState[S, A, B]) => StateEvent.Next(acc, acc.s, v)))
      ) ++ List(
        (acc: TempState[S, A, B]) => StateEvent.StashEnd(acc.s, result, element_list)
      )

      val interm = (event_fn_list.foldLeft(new_s) { (acc, ev_fn) => //(only one level of stashing allowed)
        val transition_result = transition(ev_fn(acc)) match {
          case StateTransition.IsUnstashRelated(_) => throw NestedStashException
          case res => res
        }
        handle_result(acc, transition_result)
      }).copy(stash = Nil)

      val complete = if (reprocess_element) {
        handle_result(interm, transition(
          maybe_v.map(v => StateEvent.Next(interm, interm.s, v))
            .getOrElse(StateEvent.Complete(interm, interm.s))
        ))
      } else interm
      complete.copy(unstash = None)

    case new_s => new_s
  }

  /** Additional logic for unstashing
   *  Basically there's 2 scenarios: (the full sequence is like ...,T, a, b, c, Xp, X, Nn,....)
   *  (Xp, X)[!reproc], S:(a, b, c) [Xn], gonna do "NE(X, a),  NE(a, b), NE(b, c), NE(c, Xn)"
   *  (Xp, X)[reproc],  S:(a, b, c) [Xn], gonna do "NE(Xp, a), NE(a, b), NE(b, c), NE(c, X)"
   *  TODO: the problem is I can't output element and unstash simul which is the second case needs
   * (where is gets complicated is if I'm acting on Xp not X and then eg t(Xp) > t(a), my processing actually
   *  needs to be NE(Xpp, a), ..., NE(c, Xp)
   * ^ahh but ..., a, b, c, Xpp, Xp[UNSTASH], X isn't really how it stashing is _supposed_ to work?
   *  The alternative is to do
   *   rp: SS(Xp, X, a), NE(a, b), NE(b, c), SE(c, ??), NE(??, Y)
   *  !rp: SS(Xp, X, a), NE(a, b), NE(b, c), SE(c, ??), NE(??, ??)
   * There's really 3 ways you can use foldLeft2: based on processing Xp, based on processing X,
   * or treating (X, Xp) as a combined event, though really you could use foldLeft in that case?
   * (and then you might choose to unstash based on looking forward, or looking backwards
   * ie based on that you could want
   * (processing Xp looking forward: "...,a,b,c,Xp,X")
   * (processing Xp looking backward: "Xp,a,b,c,X,...")
   * if processing X this whole thing makes no sense unless Xp==c right?
   */
  private def command_completion2[S, A, B](
    maybe_prev_v: Option[A], maybe_v: Option[A],
    transition: StateEvent2[S, A, B] => StateTransition[S, A, B],
    result: StateEvent2[S, A, B],
    result_handler: StateTransition[S, A, B] => TempState[S, A, B]
  ) : TempState[S, A, B] = (transition andThen result_handler)(result) match
  {
    case new_s @ TempState(s, _, _, stash, Some(UnstashAll(reprocess_element, remap))) =>
      /** Builds the next element based on the overall context */
      def next_element(
        curr_acc: TempState[S, A, B],
        maybe_prev_v: Option[A], maybe_v: Option[A]
      ): StateEvent2[S, A, B] = (maybe_prev_v, maybe_v) match
      {
        case (None, Some(v)) => StateEvent2.First(curr_acc, curr_acc.s, v)
        case (Some(prev_v), Some(v)) => StateEvent2.Next(curr_acc, curr_acc.s, prev_v, v)
        case (Some(prev_v), None) => StateEvent2.Complete(curr_acc, curr_acc.s, Some(prev_v))
        case (None, None) => StateEvent2.Complete(curr_acc, curr_acc.s, None)
      }
      //TODO: not sure this is still valid
      //eg say I stash (Xp, X), (a, b, c) [Y], gonna do "SS(a...), NE(X, a), NE(a, b), NE(b, c), SE(c), NE(c, Y)"
      //(Xp, X) [reprocess] -> NE(Xp,a) | (Xp,X) [!reprocess] -> NE(a, b)

      val element_list = remap(stash)
      val event_fn_list: List[TempState[S, A, B] => StateEvent2[S, A, B]] = (
        ((acc: TempState[S, A, B]) => StateEvent2.StashStart(acc.s, element_list.headOption, result, element_list))
        ::
        element_list.map(v => ((acc: TempState[S, A, B]) => next_element(acc, acc.prev, Some(v))))
      ) ++ List(
        (acc: TempState[S, A, B]) => StateEvent2.StashEnd(acc.s, element_list.lastOption, result, element_list)
      )

      //TODO: this and the TODO below are the bits to fix
      val adjusted_new_s = new_s.copy(prev = if (reprocess_element) maybe_prev_v else maybe_v)
      val interm = (event_fn_list.foldLeft(adjusted_new_s) { (acc, ev_fn) => //(only one level of stashing allowed)
        val event = ev_fn(acc)
        val transition_result = transition(event) match {
          case StateTransition.IsUnstashRelated(_) => throw NestedStashException
          case res => res
        }
        handle_result(acc, transition_result).copy(prev = event match {
          case StateEvent2.EventsThatGeneratePrevs(a) => Some(a)
          case _ => None
        })
      }).copy(stash = Nil)

      val complete = if (reprocess_element) {
        handle_result(interm, transition(
          next_element(interm, interm.prev, maybe_v) //TODO: should be command_completion2??
        ))
      } else interm
      complete.copy(prev = maybe_v, unstash = None)

    case new_s => new_s.copy(prev = maybe_v)
  }

  //TODO scanLeft, which returns the same "list" type as input together with an incremental view of the state

}
