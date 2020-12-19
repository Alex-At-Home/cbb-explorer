package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models.TeamId

object DataQualityIssues {

  /** Use first and last letters from first name for these players */
  val players_with_duplicate_names = Set(
    "mitchell, makhi", "makhi mitchell", "mitchell,makhi",
    "mitchell, makhel", "makhel mitchell", "mitchell,makhel",

    "hamilton, jared", "jared hamilton", "hamilton,jared",
    "hamilton, jairus", "jairus hamilton", "hamilton,jairus",

    // Wisconsin team-mates, leave Jordan with Jo and Jonathan gets Jn
    //"davis, jordan", "jordan davis", "davis,jordan",
    "davis, jonathan", "jonathan davis", "davis,jonathan",

// These two have the same name regardless of strategy! Use misspellings to tuncate Jaev's name
    "cumberland, jaev", "jaev cumberland", "cumberland,jaev",
    "cumberland, jarron", "jarron cumberland", "cumberland,jarron",
  )
  val misspellings: Map[Option[TeamId], Map[String, String]] = Map( // pairs - full name in box score, and also name for PbP

    // ACC:
    Option(TeamId("Virginia")) -> Map(
      //PbP tidy game from W 2020/21
      "Ti Stojsavlevic" -> "Ti Stojsavljevic",
    ),

    Option(TeamId("Duke")) -> Map(
      //Box tidy complicated game from W 2018/9
      "Akinbode-James, O." -> "James, Onome",
    ),

    Option(TeamId("Florida St.")) -> Map(
      // PBP errors (see also generic misspellings, which doesn't work for player events
      // because it doesn't change the tidy name, only fixes the code)
      "Willliams, Patrick" -> "Williams, Patrick",
      "Osbrone, Malik" -> "Osborne, Malik"
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

    Option(TeamId("Cincinnati")) -> Map(
      // The Cumberlands have caused quite a mess!
      // Truncate Jaevin's name (sorry Jaevin!)
      "CUMBERLAND,J" -> "CUMBERLAND,JARRON", //(just in case!)
      "Cumberland, Jaevin" -> "Cumberland, Jaev",
      "CUMBERLAND,JAEVIN" -> "CUMBERLAND,JAEV",
      "Jaevin Cumberland" -> "Jaev Cumberland"
    ),

    Option(TeamId("East Carolina")) -> Map(
      // PbP error
      "BARUIT,BITUMBA" -> "BARUTI,BITUMBA",
      //Box/PBP remove 2nd name
      "Doumbia, Ibrahim Famouke" -> "Doumbia, Ibrahim",
      "Ibrahim Famouke Doumbia" -> "Ibrahim Doumbia"
    ),

    Option(TeamId("UCF")) -> Map(
      // PbP error W 2018/19
      "Korneila Wright" -> "Kay Kay Wright",
      "WRIGHT,KORNEILA" -> "WRIGHT, KAY KAY",
    ),

    // Big East

    Option(TeamId("Creighton")) -> Map(
      // PbP error W 2020/21
      "Dee Dee Pryor" -> "DeArica Pryor",
    ),

    // B1G

    Option(TeamId("Iowa")) -> Map(
      // PbP error W 2020/21
      "Lauren Jense" -> "Lauren Jensen",
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

    // C-USA

    Option(TeamId("Fla. Atlantic")) -> Map(
      // Bring PbP in line with box (2020)
      "B.J. Greenlee" -> "Bryan Greenlee"
    ),

    Option(TeamId("Middle Tenn.")) -> Map(
      // Bring PbP in line with box (2020)
      "CRISS,JV" -> "MILLNER-CRISS,JO"
    ),

    // MWC

    Option(TeamId("Wyoming")) -> Map(
      // PBP has this the wrong way round compared to box score
      "LaMont Drew" -> "Drew LaMont"
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

    Option(TeamId("Oregon")) -> Map(
      // Was handled by generic misspellings but that doesn't work for player analysis, see below:
      "CAHVEZ,TAYLOR" -> "CHAVEZ,TAYLOR"
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
    //TODO: using this doesn't fix the name, only the code, which is problematic
    // Seen in PbP data for FSU 2019/20
    // So for now I've removed all the entries and added them to the known cases to the misspellings map
  )
}
