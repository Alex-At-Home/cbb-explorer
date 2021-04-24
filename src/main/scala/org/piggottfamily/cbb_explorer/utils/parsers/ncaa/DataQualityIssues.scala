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
  val players_with_duplicate_names = Set((
    // Mitchell brothers (Maryland / RI)
    combos("Makhi", "Mitchell") ++ combos("Makhel", "Mitchell") ++
    // Hamilton brothers (BC)
    combos("Jared", "Hamilton") ++ combos("Jairus", "Hamilton") ++
    // Wisconsin team-mates, leave Jordan with Jo and Jonathan gets Jn
    //"davis, jordan", "jordan davis", "davis,jordan",
    combos("Jonathan", "Davis") ++
    // Cumberland relatives (Cinci)
//TODO: when re-importing next, leave Jarron with Ja, Jaevon gets Jn and can remove from misspellings?
    // These two have the same name regardless of strategy! Use misspellings to tuncate Jaev's name
    combos("Jaev", "Cumberland") ++ combos("Jarron", "Cumberland") ++
    // Bama - Quinerly bros(?) Jahvon and Jaden, leave Jahvon with Ja, Jadon gets Jn
    combos("Jaden", "Quinerly") ++
    Nil
  ):_*).map(_.toLowerCase)

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

    Option(TeamId("Fordham")) -> Map(( //A10
      // Lots of box scores has him by this nickname, as do PbP
      alias_combos(("Josh", "Colon") -> "Navarro, Josh")
    ):_*),

    /////////////////////////////////

    // Hack to workaround duplicate name

    Option(TeamId("Cincinnati")) -> Map((
      // The Cumberlands have caused quite a mess!
      // Truncate Jaevin's name (sorry Jaevin!)
      Seq("CUMBERLAND,J" -> "Cumberland, Jarron") ++ //(some legacy typo, just in case!)
      alias_combos(("Jaevin", "Cumberland") -> "Cumberland, Jaev")
    ):_*)

  ).mapValues(
    _ ++ generic_misspellings
  ).withDefault(_ => generic_misspellings)

  /** common mispellings - currently none */
  val generic_misspellings: Map[String, String] = Map()


  // Some utils:

  /** Generate the 3 different strings: box, PbP, legacy PbP */
  def combos(first_last: (String, String)): Seq[String] = {
    val (first, last) = first_last
    List(
      s"$last, $first", s"$first $last", s"${last.toUpperCase},${first.toUpperCase}"
    )
  }
  /** Generate the 3 different strings: box, PbP, legacy PbP - for use in alias maps */
  def alias_combos(first_last_to: ((String, String), String)): Seq[(String, String)] = {
    val (first_last, to_name) = first_last_to
    combos(first_last).map(_ -> to_name)
  }
}
