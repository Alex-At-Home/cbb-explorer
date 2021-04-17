package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models.TeamId
import org.piggottfamily.cbb_explorer.models.Year

import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult

object DataQualityIssues {

  /** Will be in format "LASTNAME,FIRSTNAME" or "Lastname, Firstname" */
  val players_missing_from_boxscore: Map[TeamId, Map[Year, List[String]]] = Map(
    TeamId("Texas A&M") -> Map(
      Year(2018) -> List("Vaughn, Everett", "Gilder, Admon")
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

    Option(TeamId("Morgan St.")) -> Map( //(MEAC)
      // box name difference 2020/21 (PbP also wrong but can auto fix that)
      "Devonish, Sherwyn" -> "Devonish-Prince, Sherwyn",
      "Devonish-Prince Jr., Sherwyn" -> "Devonish-Prince, Sherwyn",
    ),

    /////////////////////////////////

    // Both PBP and BOX

    Option(TeamId("South Carolina St.")) -> Map(
      // Box is wrong, unusually 2020/21
      "JR., RIDEAU" -> "Rideau Jr., Floyd",
      // PBP:
      "23" -> "WRIGHT,JADAKISS"
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

        // 1] One false positive
        // 2] Did see false positives with 2 and 3 before I had the multi-box-score

//TODO: to fix:

// False positives
//(2018/9 Missouri) DataQualityIssues.Fixer: [Smith, Mark] [SUCCESS.1B: single strong match: [StrongSurnameMatch(Smith, Mark,70)]] (key=[Missouri/SMITH,DRU], box=[List(Yerkes, Evan, Guess, Christian, Smith, Mark, Wolf, Adam, Ford, Brooks)])
// True negatives
//(2018/9 Wichita St.) DataQualityIssues.Fixer: [NO_MATCH] [ERROR.3B: multiple near first name matches: [CHA,ISAIAH POOR] vs [List(NoSurnameMatch(Moore, Chance,None,Some(cha),[cha,isaiah poor] vs [moore, chance]: Failed to find a fragment matching [moore], candidates=(cha,0);(isaiah,0);(poor,67)))]] (key=[Wichita St./CHA,ISAIAH POOR], box=[List(Burton, Jamarius, Moore, Chance, Brown, Rod, Poor Bear-Chandler, Isaiah, McDuffie, Markis, Farrakhan, Eli, Udeze, Morris, Bush, Brycen, Herrs, Jacob)])

// Problem conferences with new code

//2019/20 atlanticten
// [LineupErrorAnalysis] Total possession errors: [217] (good: [27418])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (16,143), 4 -> (5,22), 7 -> (3,6), 6 -> (10,46))]

//2020/21 conferenceusa
// [LineupErrorAnalysis] Total possession errors: [386] (good: [24840])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (27,243), 4 -> (5,6), 6 -> (11,137))]

//2020/21 bigsouth
// [LineupErrorAnalysis] Total possession errors: [435] (good: [17837])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (25,118), 4 -> (4,1), 3 -> (1,0), 6 -> (17,316))]

//2018/9 sec
// [LineupErrorAnalysis] Total possession errors: [232] (good: [31473])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (8,41), 6 -> (60,170), 7 -> (7,14), 3 -> (1,0), 8 -> (3,7), 4 -> (1,0))]

//2018/9 american
// [LineupErrorAnalysis] Total possession errors: [194] (good: [26640])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (16,64), 6 -> (28,122), 7 -> (5,6), 3 -> (1,1), 8 -> (1,0), 4 -> (1,1))]

//*****2019/20 atlanticten
// LineupErrorAnalysis] Total possession errors: [1430] (good: [29170])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (124,1044), 6 -> (45,327), 9 -> (1,4), 2 -> (1,1), 7 -> (5,12), 3 -> (1,1), 8 -> (1,6), 4 -> (6,35))]

//******2019/20 women_sec
// [LineupErrorAnalysis] Total possession errors: [1024] (good: [29642])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (63,641), 6 -> (32,359), 2 -> (2,2), 7 -> (3,14), 3 -> (1,0), 8 -> (1,1), 4 -> (5,7))]

//2019/20 women_acc
// [LineupErrorAnalysis] Total possession errors: [150] (good: [28121])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (13,93), 4 -> (1,0), 7 -> (2,11), 6 -> (11,46))]

//******2018/19 women_sec
// [LineupErrorAnalysis] Total possession errors: [1407] (good: [30387])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(5 -> (98,957), 6 -> (54,385), 2 -> (1,0), 7 -> (6,16), 3 -> (3,4), 8 -> (1,6), 4 -> (8,39))]

//2018/20 women_acc
// [LineupErrorAnalysis] Total possession errors: [662] (good: [26161])
// [LineupErrorAnalysis] Bad lineup analysis: [Map(0 -> (1,0), 5 -> (19,114), 1 -> (1,1), 6 -> (37,274), 2 -> (2,1), 7 -> (1,0), 3 -> (4,7), 4 -> (48,265))]

//TODO: some other interesting cases:
//1] DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[East Carolina/CLAYTOR,DOMINIQUE], box=[List()])
// (in general, lots of small box scores):
//DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[Wofford/BIGELOW,ISAIAH], box=[List(Pringle, Nick, Turner, Keaton, Murphy, Storm, Appelgren, David, Gore, Jackson, Steelman, Jonathan)])
//DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[Oregon/SABALLY,NYARA], box=[List()])
//DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[Florida St./NICOLETTI,IZABELA], box=[List(Wright, Imani)])

//2] DataQualityIssues.Fixer: [NO_MATCH] [ERROR.4A: no good matches] (key=[VCU/VCU], box=[List(Santos-Silva, Marcus, Vann, Issac, Byrd, P.J., Sheehy-Guiseppi, D., Evans, Marcus, Jackson, Xavier, Curry, Keshawn, Simms, Mike'L, Jenkins, De'Riante, Gilmore, Michael)])
// (also "Team Full", other stuff)

//3] (lots of numbers)

//4] Look into "Texas A&M/GILDER,ADMON" since he wasn't on the team in 18/9 AFAICT

        case (l @ (strong :: other_strong), _, _, _) =>
          if  (other_strong.nonEmpty) {
            val context_string = s"ERROR.1A: multiple strong matches: [$candidate] vs [$l]"
            log_info(None, context_string)
            Left(context_string)
          } else {
            val context_string = s"SUCCESS.1B: single strong match: [$strong]"
            log_info(Some(strong.box_name), context_string)
            Right(strong.box_name)
          }

        case (Nil, l @ (weak :: other_weak), _, _) =>
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
          } else {
            val context_string = s"SUCCESS.3C: 'first name only' match: [$first_name_only]"
            log_info(Some(first_name_only.box_name), context_string)
            Right(first_name_only.box_name)
          }

        case _ =>
          val context_string = s"ERROR.4A: no good matches"
          log_info(None, context_string)
          Left(context_string)
      }
    }
  }
}
