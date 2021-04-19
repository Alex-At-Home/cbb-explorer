package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models.TeamId
import org.piggottfamily.cbb_explorer.models.Year

import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult

object DataQualityIssues {

//TODO: treat this like team (also seen VCU "TEAM DEF" "TEAM FULL")
//DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[TeamSeasonId(TeamId(South Dakota St.),Year(2018))/SDSUW], box=[List(Ferrand, Jordan, Theuninck, Lindsey, Hirschman, Addison, Miller, Macy, Guebert, Madison, Palmer, Sydney, Stapleton, Sydney, Bultsma, Megan, Larson, Tagyn, Irwin, Tylee, Cascio Jensen, Rylie, Selland, Myah, Burckhard, Paiton)])
// ^ mainly just need to confirm that this doesn't cause errors

//TODO:
// seen this construct a few times:
//DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[TeamSeasonId(TeamId(Kentucky),Year(2019))/O A], box=[List(Roach, Kameron, Patterson, Chasity, Anyagaligbo, Ogechi, Edwards, Dre'Una, Paschal, Amanda, King, Emma, Cole, Nae Nae, Roper, Jaida, Green, Blair, Haines, Sabrina, Merrill, Deasia, Howard, Rhyne, McKinney, KeKe, Wyatt, Tatyana)])
//DataQualityIssues.Fixer: [P, P] [SUCCESS.1B: single strong match: [StrongSurnameMatch(P, P,86)]] (key=[TeamSeasonId(TeamId(La Salle),Year(2018))/JOSEPH,DAJOUR], box=[List(Lafond, Andrew, Powell, Pookie, Beatty, David, Brookins, Miles, Spencer, Scott, Brower, Jayson, P, P, Mosely, Cheddi, Sullivan, Key, Carter, Traci, Deas, Isiah, Clark, Jack, Croswell, Ed, Phiri, Saul, Moultrie, Jamir, Kimbrough, Jared)])
//TODO: don't allow initials in roster...

//TODO 3s:
//DataQualityIssues.Fixer: [NO_MATCH] [ERROR.3B: multiple near first name matches: [CHA,ISAIAH POOR] vs [List(NoSurnameMatch(Moore, Chance,None,Some(cha),[cha,isaiah poor] vs [moore, chance]: Failed to find a fragment matching [moore], candidates=(cha,0);(isaiah,0);(poor,67)))]] (key=[TeamSeasonId(TeamId(Wichita St.),Year(2018))/CHA,ISAIAH POOR], box=[List(Haynes-Jones, Samajae, Burton, Jamarius, Torres, Ricky, Stevenson, Erik, Moore, Chance, Dennis, Dexter, Brown, Rod, Poor Bear-Chandler, Isaiah, Midtgaard, Asbjorn, Allen, Teddy, McDuffie, Markis, Farrakhan, Eli, Busse, Tate, Udeze, Morris, Bush, Brycen, Herrs, Jacob, Echenique, Jaime)])
// good one
//DataQualityIssues.Fixer: [Devonish, Sherwyn] [SUCCESS.3C: 'first name only' match: [NoSurnameMatch(Devonish, Sherwyn,Some(sherwyn),None,[sherwyn prince jr.] vs [devonish, sherwyn]: Failed to find a fragment matching [devonish], candidates=(sherwyn,27);(prince,14))]] (key=[TeamSeasonId(TeamId(Morgan St.),Year(2020))/Sherwyn Prince Jr.], box=[List(Ware, De'Torrion, Bowens, Elijah, Okafor, Victor, Vance, Thai're, Brown, Jamar, Sorber, Peter, Baxte
//**** This looks wrong
//DataQualityIssues.Fixer: [Navarro, Josh] [SUCCESS.3C: 'first name only' match: [NoSurnameMatch(Navarro, Josh,Some(josh),None,[colon, josh] vs [navarro, josh]: Failed to find a fragment matching [navarro], candidates=(colon,17);(josh,23))]] (key=[Fordham:box/Colon, Josh], box=[List(Burquest, Peter, Radovich, Luka, Skoric, Lazar, Gazi, Erten, Williams, Mason, Soriano, Joel, Portley, Antwon, Austin, Chris, Perry, Ty, Navarro, Josh, Ohams, Chuba, Rodriguez, Alec, Cohn, Cameron, Raut, Ivan, Cobb,Jalen, Eyisi, Onyi, Rose, Kyle)])
// Correctly misses this one:
//DataQualityIssues.Fixer: [NO_MATCH] [ERROR.3B: multiple near first name matches: [English, Jaren] vs [List(NoSurnameMatch(Adaway, Jalen,None,Some(jaren),[english, jaren] vs [adaway, jalen]: Failed to find a fragment matching [adaway], candidates=(english,0);(jaren,18)))]] (key=[St. Bonaventure:box/English, Jaren], box=[List(Lacewell, Malik, Johnson, Matt, Vasquez, Alejandro, Adaway, Jalen, Winston, Justin, Holmes, Jaren, Planutis, Bobby, Osunniyi, Osun, Ikpeze, Amadi, Welch, Dominick, Lofton,Kyle, Carpenter, Robert, Okoli, Alpha)])

//TODO: some guys who aren't in the roster:
// DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[TeamSeasonId(TeamId(La Salle),Year(2018))/COONEY,KYLE], box=[List(Lafond, Andrew, Powell, Pookie, Beatty, David, Brookins, Miles, Spencer, Scott, Brower, Jayson, P, P, Mosely, Cheddi, Sullivan, Key, Carter, Traci, Deas, Isiah, Clark, Jack, Croswell, Ed, Phiri, Saul, Moultrie, Jamir, Kimbrough, Jared)])
// DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[TeamSeasonId(TeamId(La Salle),Year(2018))/SHULER,JOHNNIE], box=[List(Lafond, Andrew, Powell, Pookie, Beatty, David, Brookins, Miles, Spencer, Scott, Brower, Jayson, P, P, Mosely, Cheddi, Sullivan, Key, Carter, Traci, Deas, Isiah, Clark, Jack, Croswell, Ed, Phiri, Saul, Moultrie, Jamir, Kimbrough, Jared)])
// DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[TeamSeasonId(TeamId(La Salle),Year(2018))/KUHAR,CHRIS], box=[List(Lafond, Andrew, Powell, Pookie, Beatty, David, Brookins, Miles, Spencer, Scott, Brower, Jayson, P, P, Mosely, Cheddi, Sullivan, Key, Carter, Traci, Deas, Isiah, Clark, Jack, Croswell, Ed, Phiri, Saul, Moultrie, Jamir, Kimbrough, Jared)])
// DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[TeamSeasonId(TeamId(Xavier),Year(2018))/VANDERPOHL,NICK], box=[List(Scruggs, Paul, Jones, Tyrique, Swetye, Zak, Kennedy, Keonte, Marshall, Naji, Frazier, A.J., Harden, Elias, Schrand, Leighton, James, Dontarius, Goodin, Quentin, Castlin, Kyle, Singh, Ramon, Hankins, Zach, Welage, Ryan)])


//TODO;
// This one used to be fixed when I was combining box scores?!
//DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[TeamSeasonId(TeamId(Morgan St.),Year(2020))/Lapri Pace], box=[List(Ware, De'Torrion, Bowens, Elijah, Okafor, Victor, Vance, Thai're, Brown, Jamar, Sorber, Peter, Baxter Jr, Troy, Khaalid, Naseem, Wright, Sharone, Burke, Isaiah, Miller, Malik, Grantsaan, Lagio, Venning, Chad, Marable, Josiah, Holston, Troy, Campbell, Tahj-Malik, Camara, Moel, Devonish, Sherwyn, Moore, Trevor)])

//TODO: ... really should have been allowed these 3...
// DataQualityIssues.Fixer: [NO_MATCH] [ERROR.1A: multiple strong matches: [Cumberland, Jaevin] vs [List(StrongSurnameMatch(Cumberland, Jaev,94), StrongSurnameMatch(Cumberland, Jarron,83))]] (key=[Cincinnati:box/Cumberland, Jaevin], box=[List(Banks, Rob, Cumberland, Jarron, Harvey, Zach, Cook, Adam, Vogt, Chris, Scott, Tre, Toyambi, Prince, Sorolla, Jaume, Moore, Trevor, Cumberland, Jaev, Koz, John, Davenport, Jeremiah, Martin, Sam, McNeal, Chris, Diarra, Mamoudou, Williams, Keith, Adams-Woods, Mika)])
// DataQualityIssues.Fixer: [NO_MATCH] [ERROR.1A: multiple strong matches: [Jones, Mike] vs [List(StrongSurnameMatch(Jones, Bates,70), StrongSurnameMatch(Jones, Michael,80))]] (key=[Davidson:box/Jones, Mike], box=[List(Kristensen, David, Freundlich, Cal, Casey, Patrick, Dibble, Drew, Jones, Michael, Collins, Carter, Brajkovic, Luka, Wynter, Malcolm, Boachie-Yiadom, Nelson, Lee, Hyunjung, Jones, Bates, Frampton, Luke, Grady, Kellan, Czerapowicz, David, Pritchett, Kishawn, Gudmunsson, Jon Axel)])
// DataQualityIssues.Fixer: [NO_MATCH] [ERROR.1A: multiple strong matches: [DOOITTLE,TRAVONTA] vs [List(StrongSurnameMatch(Doolittle, Daveon,76), StrongSurnameMatch(Doolittle, Travonta,94))]] (key=[TeamSeasonId(TeamId(Ark.-Pine Bluff),Year(2020))/DOOITTLE,TRAVONTA], box=[List(Posey, Cameron, Doolittle, Travonta, Johnson, Joshuwan, Hargrove, Kobe, Stredic Jr, Alvin, Stokes , Kshun, Parchman, Omar, Woods, Rylan, Morris, Dequan, Jones, Nicholas, Lynn, Jalen, Boyd , Robert, Banyard, Terrance, Bell, Markedric, Doss Jr., Shaun, Ivory III, George, Doolittle, Daveon)])


  /** Will be in format "LASTNAME,FIRSTNAME" or "Lastname, Firstname" */
  val players_missing_from_boxscore: Map[TeamId, Map[Year, List[String]]] = Map(
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

  object Fixer {

    sealed trait MatchResult { def box_name: String }
    case class NoSurnameMatch(box_name: String, exact_first_name: Option[String], near_first_name: Option[String], err: String) extends MatchResult
    case class WeakSurnameMatch(box_name: String, score: Int, info: String) extends MatchResult
    case class StrongSurnameMatch(box_name: String, score: Int) extends MatchResult

    val min_surname_score = 80
    val min_first_name_score = 75
    val min_overall_score = 70
    val min_useful_surname_len = 4
    val min_useful_first_name_len = 3

    /** Given a PbP name and a single box name, determine how close they are to fitting */
    def box_aware_compare(candidate_in: String, box_name_in: String): MatchResult = {
      val candidate = candidate_in.toLowerCase
      val box_name = box_name_in.toLowerCase

      def remove_jr(s: String) = s != "jr."

      val box_name_decomp = box_name.split("\\s*,\\s*", 2) // (box is always in "surname-set, other-name-set")
      val longest_surname_fragment =
        box_name_decomp(0).split(" ").toList
          .filter(remove_jr).sortWith(_.length > _.length).headOption.getOrElse("unknown")

      val candidate_frags = candidate.split("[, ]+").filter(remove_jr)

      val candidate_frag_scores = candidate_frags.map { frag =>
        frag -> FuzzySearch.weightedRatio(frag, longest_surname_fragment)
      }
      val best_frag_score = candidate_frag_scores.filter {
        case (frag, _) if (longest_surname_fragment == frag) && frag.length >= (min_useful_surname_len - 1) => true
          //(more permissive if they are actually ==)
        case (frag, score) if score > min_surname_score && frag.length >= min_useful_surname_len => true
        case _ => false
      }.sortWith(_._2 > _._2).headOption

      best_frag_score match {
        case Some((frag, _)) =>
          val overall_score = FuzzySearch.weightedRatio(candidate, box_name)
          if (overall_score >= min_overall_score)
            StrongSurnameMatch(box_name_in, overall_score)
          else
            WeakSurnameMatch(box_name_in, overall_score, s"[$candidate] vs [$box_name]: " +
              s"Matched [$longest_surname_fragment] with [$best_frag_score], but overall score was [$overall_score]"
            )
        case None => // We will record cases where there is a strong first name match
          //(note we don't bother recording the surname strength in this case since empirically they seem a bit random below 60ish)
          val candidate_frag_set = candidate_frags.toSet
          val box_first_names = box_name_decomp.toList.drop(1).headOption.map(_.split("[, ]+").toList.filter(remove_jr))
          val maybe_exact_first_name = box_first_names.collect {
            case List(single_first_name) //needs to be strong enough and match
              if single_first_name.length >= min_useful_first_name_len && candidate_frag_set(single_first_name) => single_first_name
          }
          val maybe_near_first_name = box_first_names match {
            case _ if maybe_exact_first_name.nonEmpty => None
            case Some(List(single_first_name)) if single_first_name.length >= min_useful_first_name_len =>
              candidate_frags.filter(FuzzySearch.weightedRatio(_, single_first_name) >= min_first_name_score).headOption
            case _ => None
          }
          NoSurnameMatch(box_name_in, maybe_exact_first_name, maybe_near_first_name, s"[$candidate] vs [$box_name]: " +
            s"Failed to find a fragment matching [$longest_surname_fragment], candidates=${candidate_frag_scores.mkString(";")}"
          )
      }
    }

    /** Only used to reduce diagnosis prints */
    var fixes_for_debug: Map[String, (String, String, Boolean)] = Map()

    /** The top level method for finding a reliable box score name for a mis-spelled PbP name */
    def fuzzy_box_match(candidate: String, unassigned_box_names: List[String], team_context: String): Either[String, String] = {

      val matches = unassigned_box_names.map(box_aware_compare(candidate, _))

      val init:
        (List[StrongSurnameMatch], List[WeakSurnameMatch], List[NoSurnameMatch], List[NoSurnameMatch]) = (Nil, Nil, Nil, Nil)

      val match_info = matches.foldLeft(init) {
        case (state, p: StrongSurnameMatch) => (p :: state._1, state._2, state._3, state._4)
        case (state, p: WeakSurnameMatch) => (state._1, p :: state._2, state._3, state._4)
        case (state, p: NoSurnameMatch) if p.exact_first_name.nonEmpty => (state._1, state._2, p :: state._3, state._4)
        case (state, p: NoSurnameMatch) => (state._1, state._2, state._3, p :: state._4)
      }

      /** Log once per result (unless you get different matches) */
      def log_info(maybe_result: Option[String], context: String): Unit = {
        val prefix = "DataQualityIssues.Fixer"
        val result = maybe_result.getOrElse("NO_MATCH")
        val key = s"$team_context/$candidate"
        fixes_for_debug.get(key) match {
          case Some((_, _, true)) => //(always do nothing here)
          case Some((curr_result, curr_context, false)) if result != curr_result =>
            println(s"$prefix: ERROR.X: [$result] vs [$curr_result] ([$context] vs [$curr_context]) (key=[$key], box=[$unassigned_box_names])")
            fixes_for_debug = fixes_for_debug + (key -> (curr_result, context, true)) //(overwrite so only display this error once)
          case Some((`result`, _, _)) => // nothing to do
          case _ =>
            println(s"$prefix: [$result] [$context] (key=[$key], box=[$unassigned_box_names])")
            fixes_for_debug = fixes_for_debug + (key -> (result, context, true))
        }
      }

      match_info match {

        // Options:
        // 1] There is a single strong match (0+ weak matches): pick that
        // 2] There is a single weak match: pick that
        // 3] There are 0 weak matches, but the first name matches and is not similar to any strings occurring elsewhere

        case (l @ (strong :: other_strong), _, _, _) =>
          if  (other_strong.nonEmpty) {
            l.sortWith(_.score > _.score) match {
              case best_strong :: less_good_strong =>
                val threshold_score = best_strong.score - 10
                val new_candidates = less_good_strong.filter(_.score > threshold_score)

                if (less_good_strong.nonEmpty) {
                  val context_string = s"ERROR.1A: multiple strong matches: [$candidate] vs [$l]"
                  log_info(None, context_string)
                  Left(context_string)
                } else {
                  val context_string = s"SUCCESS.1B: multiple strong matches, but clear winner: [$candidate] vs [$l]"
                  log_info(Some(strong.box_name), context_string)
                  Right(strong.box_name)
                }

              case l2 @ _ => Left(s"(internal logic error: [$l2])")
            }
          } else {
            val context_string = s"SUCCESS.1C: single strong match: [$strong]"
            log_info(Some(strong.box_name), context_string)
            Right(strong.box_name)
          }

        case (Nil, l @ (weak :: other_weak), _, _) =>

          // Can generate false positives: the "sibling who is a walk-on and gets a few minutes" but never
          // makes it onto a box score (Example: Oregon 2018: Sabally, Satou/Nyara)
          // (likely this shouldn't cause much inaccuracy ... if they played so little then they probably
          //  won't kill their sibling's stats!)

          if  (other_weak.nonEmpty) {
            val context_string = s"ERROR.2A: multiple weak matches: [$candidate] vs [$l]"
            log_info(None, context_string)
            Left(context_string)
          } else {
            val context_string = s"SUCCESS.2B: single weak match: [$weak]"
            log_info(Some(weak.box_name), context_string)
            Right(weak.box_name)
          }

        case (Nil, Nil, l @ (first_name_only :: other_first_name_only), l2) =>
          if (other_first_name_only.nonEmpty) {
            val context_string = s"ERROR.3A: multiple first name matches: [$candidate] vs [$l]"
            log_info(None, context_string)
            Left(context_string)
          } else if (l2.exists(_.near_first_name.nonEmpty)) {
            // We have to do one final check: are there any _fuzzy_ matches to the first name
            // (we're being pretty cautious in our recklessness here!!)
            val bad_l2 = l2.filter(_.near_first_name.nonEmpty)
            val context_string = s"ERROR.3B: multiple near first name matches: [$candidate] vs [$bad_l2]"
            log_info(None, context_string)
            Left(context_string)
          } else { // After some reflection I'm retiring this for now - too many false positives empirically
            //TODO: note didn't quite work anyway, see: [CHA,ISAIAH POOR] vs ""[List(NoSurnameMatch(Moore, Chance,None,Some(cha),[cha,isaiah poor] vs [moore, chance]""
            //(used wrong surname to get first name)
            val context_string = s"ERROR.3C: 'first name only' match: [$first_name_only]"
            log_info(Some(first_name_only.box_name), context_string)
            //Right(first_name_only.box_name)
            Left(context_string)
          }

        case _ =>
          val context_string = s"ERROR.4A: no good matches"
          log_info(None, context_string)
          Left(context_string)
      }
    }
  }
}
