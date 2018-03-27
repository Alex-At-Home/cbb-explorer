package org.piggottfamily.cbb_explorer.utils

object TestUtils {

  /** Like a match statement but throws an error if no cases apply */
  def inside[T](t: => T)(pf: PartialFunction[T, Unit]): Unit = {
    val fallthrough: PartialFunction[T, Unit] = {
      case non_matching =>
        throw new Exception(s"Failed to match inside [$non_matching]")
    }
    (pf orElse fallthrough).apply(t)
  }
}
