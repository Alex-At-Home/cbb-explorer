package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

object DataQualityIssues {

  /** Use first and last letters from first name for these players */
  val playersWithDuplicateNames = Set(
    "mitchell, makhi", "makhi mitchell",
    "mitchell, makhel", "makhel mitchell"
  )
}
