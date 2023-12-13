package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models.TeamId
import org.piggottfamily.cbb_explorer.models.Year

object DataQualityIssues {

  /** Had teams change names mid-season */
  val team_aliases: Map[Year, Map[TeamId, TeamId]] = Map(
    Year(2021) -> Map(
      TeamId("NIU") -> TeamId("Northern Ill.")
    )
  )

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

    // These caused issues with box/PbP parsing:

    // Mitchell brothers (Maryland 19/20 / RI 20/21)
    combos("Makhi", "Mitchell") ++ combos("Makhel", "Mitchell") ++
    // Hamilton brothers (BC 18-19/20)
    combos("Jared", "Hamilton") ++ combos("Jairus", "Hamilton") ++
    // Wisconsin team-mates, give Jordan "Jn" and Jonathan/Johnny gets "Jo"
// TODO: need to reimport 2020/21 where I did this the other way round for some reason (Jonathan is Jn)
    combos("Jordan", "Davis") ++ 
    // Cumberland relatives (Cinci)
    // These two have the same name regardless of strategy! Use misspellings to tuncate Jaev's name
    // (these are both major players so don't have favorites)
//TODO: just to Jaevin -> Jn and simplify misspellings
    combos("Jaev", "Cumberland") ++ combos("Jarron", "Cumberland") ++

    // These caused issues during roster import:

    // Men 20/21

    // Bama 20/21 - Quinerly bros(?) Jahvon and Jaden, leave Jahvon with Ja, Jadon gets Jn
    combos("Jaden", "Quinerly") ++
    // Wichita St 20/21 - Trey and Trevin Wade. Trevin gets Tn
    combos("Trevin", "Wade")  ++
    // Ohio 20/21 - Miles and Michael Brown. Micheal gets  Ml
    combos("Michael", "Brown") ++

    // Men 22/23

    // Arizona St. 2022/23, 2x Cambridge transfers one played ~20mpg at Auburn, other 30mpg at Nevada (change both)
//TODO: will make the history view in hoop-explorer not work    
    combos("Devan", "Cambridge") ++ combos("Desmond", "Cambridge Jr.") ++

    // Kansas City, 2022/23, Precious and Promise Idiaru ... we'll change both
    combos("Precious", "Idiaru") ++ combos("Promise", "Idiaru") ++

    // CSU Bakersfield 2022/23, Kareem and Kaseem Watson
    // see also under misspellings, go with Kareem->Ka, Kaseem->Ks
    combos("Kas", "Watson") ++ 

    // Seton Hall 2022/23, new! JaQuan Harris, old Jamir Harris
    combos("JaQuan", "Harris") ++ 

    // Southern U 2022/23, Jaronn+Jariyon Wilkens, neither played D1 last year
    // see also under misspellings, go with Jariyon->(Jariy)->Jy, Jaronn can jeep Ja.
    combos("Jariy", "Wilkens") ++ 

    // Men 23/24

    // Colorado St. 2023/24, Kyle and Kyan Evans. Kyan's a Fr, sorry kid
    combos("Kyan", "Evans") ++ 

    // Women 18/19

    // Women 2018 Wash St. Molina - Chanelle v Cherilyn (leave Chanelle else will conflict with 3rd sister Celena!)
    combos("Cherilyn", "Molina") ++

    // Women 19/20

    // Women 2019 Cinci - Scott, Jadyn / Jada
    combos("Jadyn", "Scott") ++ combos("Jada", "Scott") ++
    // Women 2019 Memphis - Williams, Lanetta / Lanyce (->Le)
    combos("Lanyce", "Williams") ++

    // Women 20/21

    // Gonzaga 2020/21 (W) "Truong, Kaylynne" and "Truong, Kayleigh" - Kayleigh is the starter
    combos("Kaylynne", "Truong") ++

    // Women 21/22

    // Florida 2021/22 Tatiana and Taliyah Wyche ... make Taliyah be Th because otherwise they are both Ta!
    combos("Taliyah", "Wyche") ++
    // Syracuse 2021/22 Christianna and Chrislyn Carr .. both starting! Sorry Chrislyn :)
    combos("Chrislyn", "Carr") ++

    // Women 22/23

    // Miami 22/23: Haley and Hannah Cavinder, both from Fresno St so change both since neither in DB currently
    combos("Haley", "Cavinder") ++ combos("Hannah", "Cavinder") ++

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

    Option(TeamId("BYU")) -> Map(( //(MWC)
      // roster/box name difference women - 2020/21 (using the longer version allows PbP to work)
      //TODO: should be able to handle this surname/first-name mismatch automatically?
      alias_combos(("Babalu", "Ugwu") -> "Stewart, Babalu")
    ):_*),

    Option(TeamId("Morgan St.")) -> Map( //(MEAC)
      // roster/box name difference 2020/21 (using the longer version allows PbP to work)
      "Devonish, Sherwyn" -> "Devonish-Prince, Sherwyn"
    ),

    Option(TeamId("NJIT")) -> Map( //(America East)
      // box score has this misspelling 23/24
      "Lewal, Levi" -> "Lawal, Levi"
    ),

    Option(TeamId("Morgan St.")) -> Map( //(MEAC)
      // roster/box name difference 2020/21 (using the longer version allows PbP to work)
      "Devonish, Sherwyn" -> "Devonish-Prince, Sherwyn"
    ),

    Option(TeamId("Southern California")) -> Map( //(PAC-12)
      // roster/box name difference 2023/24 (NCAA is confused whether to use married name or not)
      "Darius, Dominique" -> "Onu, Dominique"
    ),

    /////////////////////////////////

    // Both PBP and BOX

    Option(TeamId("Fordham")) -> Map(( //A10
      // Lots of box scores has him by this nickname (Josh "Colon" Navarro), as do PbP
      alias_combos(("Josh", "Colon") -> "Navarro, Josh")
    ):_*),

    /////////////////////////////////

    // Verbal commits

    Option(TeamId("Oral Roberts")) -> Map(( //Summit
      // Lots of box scores has him by this nickname (Josh "Colon" Navarro), as do PbP
      alias_combos(("Max", "Abams") -> "Abmas, Max")
    ):_*),


    /////////////////////////////////

    // Hack to workaround duplicate name

    Option(TeamId("Cincinnati")) -> Map((
      // The Cumberlands have caused quite a mess!
      // Truncate Jaevin's name (sorry Jaevin!)
      Seq("CUMBERLAND,J" -> "Cumberland, Jarron") ++ //(some legacy typo, just in case!)
      alias_combos(("Jaevin", "Cumberland") -> "Cumberland, Jaev")
    ):_*),

    Option(TeamId("CSU Bakersfield")) -> Map((
      // The Watsons have caused quite a mess!
      // Truncate Kaseem's name (sorry Kaseem!)
      alias_combos(("Kaseem", "Watson") -> "Watson, Kas")
    ):_*),

    Option(TeamId("Southern U.")) -> Map((
      // The Wilkens have caused quite a mess!
      // Truncate Jariyon's name (sorry Jariyon!)
      alias_combos(("Jariyon", "Wilkens") -> "Wilkens, Jariy")
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
