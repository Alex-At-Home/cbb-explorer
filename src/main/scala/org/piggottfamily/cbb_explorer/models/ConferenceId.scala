package org.piggottfamily.cbb_explorer.models

/**
 * Represents a CBB conference
 * @param name The unique name of the conference
 */
case class ConferenceId(name: String) extends AnyVal

/** Utils related to Conference */
object ConferenceId {
  def is_high_major(c: ConferenceId) = c.name match {
    case "Atlantic Coast Conference" | "Big Ten Conference" | "Big East Conference" => true
    case "Southeastern Conference" | "Big 12 Conference" | "Pac 12 Conference" => true
    case "Pac 10 Conference" => true
    case _ => false
  }
}
