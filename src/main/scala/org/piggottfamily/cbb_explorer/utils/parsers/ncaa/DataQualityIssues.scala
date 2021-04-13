package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models.TeamId
import org.piggottfamily.cbb_explorer.models.Year

object DataQualityIssues {

  /** Will be in format "LASTNAME,FIRSTNAME" or "Lastname, Firstname" */
  val players_missing_from_boxscore: Map[TeamId, Map[Year, List[String]]] = Map(
    TeamId("Morgan St.") -> Map(
      Year(2020) -> List("McCray-Pace, Lapri")
    ),
    TeamId("South Carolina St.") -> Map(
      Year(2020) -> List("Butler, Rashamel")
    ),
    TeamId("Charleston So.") -> Map(
      Year(2020) -> List("Bowser, Sadarius")
    ),
    TeamId("High Point") -> Map(
      Year(2020) -> List("Ranleman, Bryant")
    ),
    TeamId("Longwood") -> Map(
      Year(2020) -> List("Nkereuwem, Leslie", "Stefanovic, Ilija")
    ),
    TeamId("Presbyterian") -> Map(
      Year(2020) -> List("Graham, Zeb")
    ),
    TeamId("Alcorn") -> Map(
      Year(2020) -> List("Pierce, David")
    ),
    TeamId("Ark.-Pine Bluff") -> Map(
      Year(2020) -> List("Stredic Jr., Alvin", "Stokes, Kshun", "Doss Jr., Shaun")
    ),
    TeamId("Mississippi Val.") -> Map(
      Year(2020) -> List("Gordon, Devin")
    ),
    TeamId("Southern U.") -> Map(
      Year(2020) -> List("Henderson, Harrison", "Williams Jr., Terrell")
    )
  )

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

  /** Will be in format "LASTNAME,FIRSTNAME" (old box, pbp) or "Lastname, Firstname" (new box)
   *  "Firstname Lastname" (new pbp)
   */
  val misspellings: Map[Option[TeamId], Map[String, String]] = Map( // pairs - full name in box score, and also name for PbP

    //////////////////////////////////////////////////////////////////

    // PBP Mispellings:

    /////////////////////////////////

    // Too hard to resolve

    Option(TeamId("Alcorn")) -> Map( //(SWAC)
      // Wrong in the PBP, 2020/21
      "10" -> "WILSON,KOBE"
    ),

    Option(TeamId("Ark.-Pine Bluff")) -> Map( //(SWAC)
      // Wrong in the PBP, 2020/21
      "PATTERSON,OMAR" -> "PARCHMAN,OMAR",
    ),

    Option(TeamId("Mississippi Val.")) -> Map( //(SWAC)
      // Wrong in the PBP, 2020/21
      "Jonathan Fanard" -> "Donalson Fanord",
    ),

    /////////////////////////////////

    // BOX Mispellings

    Option(TeamId("Duke")) -> Map( //(ACC)
      //Box tidy complicated game from W 2018/9
      "Akinbode-James, O." -> "James, Onome",
    ),

    Option(TeamId("La Salle")) -> Map( //(A10)
      // Wrong in the box
      "Sullivan, Key" -> "Sullivan, Cian"
    ),

    Option(TeamId("TCU")) -> Map( //(B12)
      // (wrong in box score only)
      "Ascieris, Owen" -> "Aschieris, Owen",
    ),

    Option(TeamId("Texas")) -> Map( //(B12)
      // (wrong in box score only)
      "Ostekowski, Dylan" -> "Osetkowski, Dylan",
    ),

    /////////////////////////////////

    // Both PBP and BOX

    Option(TeamId("Syracuse")) -> Map( //(ACC)
      //Box tidy complicated game from W 2019/20
      "Finklea-Guity, Amaya" -> "Guity, Amaya",
      // PBP incorrectness
      "FINKLEA,AMAYA" -> "GUITY,AMAYA"
    ),

    /////////////////////////////////

    // Hack to workaround duplicate name

    Option(TeamId("Cincinnati")) -> Map(
      // The Cumberlands have caused quite a mess!
      // Truncate Jaevin's name (sorry Jaevin!)
      "CUMBERLAND,J" -> "CUMBERLAND,JARRON", //(just in case!)
      "Cumberland, Jaevin" -> "Cumberland, Jaev",
      "CUMBERLAND,JAEVIN" -> "CUMBERLAND,JAEV",
      "Jaevin Cumberland" -> "Jaev Cumberland"
    )

  ).mapValues(
    _ ++ generic_misspellings
  ).withDefault(_ => generic_misspellings)

  /** common mispellings - currently none */
  val generic_misspellings: Map[String, String] = Map()
}
