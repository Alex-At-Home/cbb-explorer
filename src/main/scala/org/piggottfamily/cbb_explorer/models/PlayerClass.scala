package org.piggottfamily.cbb_explorer.models

/** Which year (active only), and if the player redshirted - may not know this */
case class PlayerClass(year: Int, redshirted: Option[Boolean] = None)

/** Utilities related to player class */
object PlayerClass {

  val freshman = PlayerClass(1)
  val sophomore = PlayerClass(2)
  val junior = PlayerClass(3)
  val senior = PlayerClass(4)

  def is_upperclassman(c: PlayerClass): Boolean = c.year >= 3
  def is_underclassman(c: PlayerClass): Boolean = c.year <= 2
}
