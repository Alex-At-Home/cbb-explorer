package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

object DataQualityIssues {

  /** Use first and last letters from first name for these players */
  val players_with_duplicate_names = Set(
    "mitchell, makhi", "makhi mitchell",
    "mitchell, makhel", "makhel mitchell"
  )
  val misspellings = Map( // pairs - full name in box score, and also name for PbP
    "Ostekowski, Dylan" -> "Osetkowski, Dylan",
    "ostekowski" -> "osetkowski",

    "Ascieris, Owen" -> "Aschieris, Owen",
    "ascieris" -> "aschieris"
  )
}
