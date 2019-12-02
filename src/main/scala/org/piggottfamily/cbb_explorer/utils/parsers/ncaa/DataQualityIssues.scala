package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

object DataQualityIssues {

  /** Use first and last letters from first name for these players */
  val players_with_duplicate_names = Set(
    "mitchell, makhi", "makhi mitchell", "mitchell,makhi",
    "mitchell, makhel", "makhel mitchell", "mitchell,makhel",

    "hamilton, jared", "jared hamilton", "hamilton,jared",
    "hamilton, jairus", "jairus hamilton", "hamilton,jairus",
  )
  val misspellings = Map( // pairs - full name in box score, and also name for PbP

    // B12:

    // (wrong in box score only, but easy enough to add a fix for any box score typos)
    "Ostekowski, Dylan" -> "Osetkowski, Dylan",
    "ostekowski" -> "osetkowski",

    // (wrong in box score only, but easy enough to add a fix for any box score typos)
    "Ascieris, Owen" -> "Aschieris, Owen",
    "ascieris" -> "aschieris",

    // A10:

    // Wrong in the box
    "Sullivan, Key" -> "Sullivan, Cian",

    // Wrong in the PbP, box score is correct
    "gudmunsson" -> "gudmundsson",

    // Wrong in the PbP, box score is correct
    //(sufficient for getting the box score correct)
    "longpr??" -> "longpre",

    // SEC:

    // Wrong in the PBP
    "Jordan Philips" -> "Jordan Phillips",
    "PHILIPS,JORDAN" -> "PHILLIPS,JORDAN",

    // Translit the version in the box score and PbP:
    "Sargiūnas, Ignas" -> "Sargiunas, Ignas",
    "sargiūnas" -> "sargiunas"
  )
}
