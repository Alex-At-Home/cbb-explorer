package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models.TeamId
import org.piggottfamily.cbb_explorer.models.Year

object DataQualityIssues {

  /** Will be in format "LASTNAME,FIRSTNAME" or "Lastname, Firstname" */
  val players_missing_from_boxscore: Map[TeamId, Map[Year, List[String]]] = Map(
    TeamId("La Salle") -> Map( //A10
      Year(2018) -> List("Cooney, Kyle", "Shuler, Johnnie", "Kuhar, Chris", "Joseph, Dajour")
    ),
    TeamId("Morgan St.") ->  Map( //MEAC
      Year(2020) -> List("McCray-Pace, Lapri")
    ),
    TeamId("Xavier") -> Map( //BE
      Year(2018) -> List("Vanderpohl, Nick")
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

    Option(TeamId("Ark.-Pine Bluff")) -> Map( //(SWAC)
      // Wrong in the PBP, 2019/29
      "PATTERSON,OMAR" -> "Parchman, Omar"
    ),

    Option(TeamId("Wichita St.")) -> Map( //(AAC)
      // Wrong in the PBP, 2018/19
      "CHA,ISAIAH POOR" -> "Poor Bear-Chandler, Isaiah"
    ),

    /////////////////////////////////

    // Roster/BOX Mispellings

    Option(TeamId("Morgan St.")) -> Map( //(MEAC)
      // roster/box name difference 2020/21 (using the longer version allows PbP to work)
      "Devonish, Sherwyn" -> "Devonish-Prince, Sherwyn"
    ),

    /////////////////////////////////

    // Both PBP and BOX

    Option(TeamId("Fordham")) -> Map( //A10
      // Lots of box scores has him by this nickname, as do PbP
      "Colon, Josh" -> "Navarro, Josh",
      "COLON,JOSH" -> "Navarro, Josh",
      "Josh Colon" -> "Navarro, Josh",
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
