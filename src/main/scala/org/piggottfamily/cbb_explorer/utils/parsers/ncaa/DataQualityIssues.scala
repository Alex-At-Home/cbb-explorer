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

    // ACC:
    Option(TeamId("Duke")) -> Map(
      //Box tidy complicated game from W 2018/9
      "Akinbode-James, O." -> "James, Onome",
    ),

    Option(TeamId("Georgia Tech")) -> Map(
      //PBP fix complicated game from W 2019/20
      "Nerea Hermosa Monreal" -> "Nerea Hermosa",
      // PBP 2018/9
      "DIXON,LIZ" -> "DIXON,ELIZABETH"
    ),

    Option(TeamId("Syracuse")) -> Map(
      //Box tidy complicated game from W 2019/20
      "Finklea-Guity, Amaya" -> "Guity, Amaya",
      "FINKLEA,AMAYA" -> "GUITY,AMAYA"
    ),

    // American

    Option(TeamId("East Carolina")) -> Map(
      // PbP error
      "BARUIT,BITUMBA" -> "BARUTI,BITUMBA",
      //Box/PBP remove 2nd name
      "Doumbia, Ibrahim Famouke" -> "Doumbia, Ibrahim",
      "Ibrahim Famouke Doumbia" -> "Ibrahim Doumbia"
    ),

    // B12:

    Option(TeamId("Oklahoma St.")) -> Map(
      //PBP fix complicated game from W 2019/20
      "DELAPP,KASSIDY" -> "DE LAPP,KASSIDY",
      // PBP fix
      "DESOUSA,CLITAN" -> "DE SOUSA,CLITAN"
    ),

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

    // PAC-12

    Option(TeamId("Washington")) -> Map(
      // PBP errors (W 2019/20)
      "BANBERGER,ALI" -> "BAMBERGER,ALI",
      "WALKINS,TT" -> "WATKINS,TT"
    ),

    Option(TeamId("California")) -> Map(
      // PBP errors (W 2019/20)
      "SCHIPH,EVELIEN LUTJE" -> "SCHIPHOLT,EVELIEN LUTJE"
    ),

    Option(TeamId("Colorado")) -> Map(
      // PBP errors (W 2018/19)
      "TUITELE,SIRENA" -> "TUITELE,PEANUT",
      "Sirena Tuitele" -> "Peanut Tuitele",
      "HOLLINSHED,MYA" -> "HOLLINGSHED,MYA"
    ),

    // SEC:

    // Wrong in the PBP
    Option(TeamId("Arkansas")) -> Map(
      "Jordan Philips" -> "Jordan Phillips",
      "PHILIPS,JORDAN" -> "PHILLIPS,JORDAN"
    ),

    Option(TeamId("South Carolina")) -> Map(
      "HERBERT HARRIGAN,M" -> "HERBERT HARRIGAN,MIKIAH",
      "HARRIGAN,M HERBERT" -> "HERBERT HARRIGAN,MIKIAH",
    ),

  ).mapValues(
    _ ++ generic_misspellings
  ).withDefault(_ => generic_misspellings)
  /** common mispellings */
  val generic_misspellings: Map[String, String] = Map(
    // Seen in PbP data for FSU 2019/20
    "osbrone" -> "osborne",
    "willliams" -> "williams",
    "cahvez" -> "chavez"
  )
}
