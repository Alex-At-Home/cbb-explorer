package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models.TeamId

object DataQualityIssues {

  /** Use first and last letters from first name for these players */
  val players_with_duplicate_names = Set(
    "mitchell, makhi", "makhi mitchell", "mitchell,makhi",
    "mitchell, makhel", "makhel mitchell", "mitchell,makhel",

    "hamilton, jared", "jared hamilton", "hamilton,jared",
    "hamilton, jairus", "jairus hamilton", "hamilton,jairus"
  )
  val misspellings: Map[Option[TeamId], Map[String, String]] = Map( // pairs - full name in box score, and also name for PbP

    // American

    Option(TeamId("East Carolina")) -> Map(
      // PbP error
      "BARUIT,BITUMBA" -> "BARUTI,BITUMBA",
      //Box/PBP remove 2nd name
      "Doumbia, Ibrahim Famouke" -> "Doumbia, Ibrahim",
      "Ibrahim Famouke Doumbia" -> "Ibrahim Doumbia"
    ),

    // B12:

    Option(TeamId("Texas")) -> Map(
      // (wrong in box score only, but easy enough to add a fix for any box score typos)
      "Ostekowski, Dylan" -> "Osetkowski, Dylan",
      "ostekowski" -> "osetkowski"
    ),

    // (wrong in box score only, but easy enough to add a fix for any box score typos)
    Option(TeamId("TCU")) -> Map(
      "Ascieris, Owen" -> "Aschieris, Owen",
      "ascieris" -> "aschieris"
    ),

    // A10:

    Option(TeamId("La Salle")) -> Map(
      // Wrong in the box
      "Sullivan, Key" -> "Sullivan, Cian"
    ),

    // Wrong in the PbP, box score is correct
    Option(TeamId("Davidson")) -> Map(
      "gudmunsson" -> "gudmundsson"
    ),

    // Wrong in the PbP, box score is correct
    //(sufficient for getting the box score correct)
    Option(TeamId("Saint Joseph's")) -> Map(
      "longpr??" -> "longpre"
    ),

    // SEC:

    // Wrong in the PBP
    Option(TeamId("Arkansas")) -> Map(
      "Jordan Philips" -> "Jordan Phillips",
      "PHILIPS,JORDAN" -> "PHILLIPS,JORDAN"
    )

  ).mapValues(
    _ ++ generic_misspellings
  ).withDefault(_ => generic_misspellings)
  /** common mispellings */
  val generic_misspellings: Map[String, String] = Map(
    // Seen in PbP data for FSU 2019/20
    "osbrone" -> "osborne",
    "willliams" -> "williams"
  )
}
