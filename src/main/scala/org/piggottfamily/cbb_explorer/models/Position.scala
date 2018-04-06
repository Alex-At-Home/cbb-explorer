package org.piggottfamily.cbb_explorer.models

/** One of the positions a player has played during a season/career */
case class Position(value: String) extends AnyVal

object Position {
  val point_guard = Position("PG")
  val shooting_guard = Position("SG")
  val small_forward = Position("SF")
  val power_forward = Position("PF")
  val center = Position("C")

  val front_court: Set[Position] = Set(power_forward, center)
  val back_court: Set[Position] = Set(point_guard, shooting_guard, small_forward)
}
