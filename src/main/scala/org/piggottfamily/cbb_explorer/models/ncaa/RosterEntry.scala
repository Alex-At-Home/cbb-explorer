package org.piggottfamily.cbb_explorer.models.ncaa

import org.piggottfamily.cbb_explorer.models._

/** Represents an entry in an NCAA roster
  * @param player_id
  *   The player name and code
  * @param number
  *   The jersey number
  */
case class RosterEntry(
    player_code_id: LineupEvent.PlayerCodeId,
    number: String,
    pos: String,
    height: String,
    height_in: Option[Int],
    year_class: String,
    gp: Int,
    origin: Option[String],
    role: Option[String]
)

object RosterEntry {
  val height_regex = "([0-9]+)[-]([0-9]+)".r
}
