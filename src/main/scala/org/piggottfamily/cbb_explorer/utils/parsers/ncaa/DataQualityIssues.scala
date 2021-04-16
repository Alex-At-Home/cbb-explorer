package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models.TeamId
import org.piggottfamily.cbb_explorer.models.Year

import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult

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

    /**TODO: may not be needed?

    Option(TeamId("Ark.-Pine Bluff")) -> Map( //(SWAC)
      // Wrong in the PBP, 2020/21
      "PATTERSON,OMAR" -> "PARCHMAN,OMAR",
    ),

    Option(TeamId("Mississippi Val.")) -> Map( //(SWAC)
      // Wrong in the PBP, 2020/21
      "Jonathan Fanard" -> "Donalson Fanord",
    ),
*/
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

/**TODO: may not be needed?

    Option(TeamId("Syracuse")) -> Map( //(ACC)
      //Box tidy complicated game from W 2019/20
      "Finklea-Guity, Amaya" -> "Guity, Amaya",
      // PBP incorrectness
      "FINKLEA,AMAYA" -> "GUITY,AMAYA"
    ),
*/
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

    /** Given a PbP name and a single box name, determine how close they are to fitting */
    def box_aware_compare(candidate: String, box_name: String): MatchResult = {
      val min_useful_name_len = 4

      val box_name_decomp = box_name.split("\\s*,\\s*", 2) // (box is always in "surname-set, other-name-set")
      val longest_surname_fragment =
        box_name_decomp(0).split(" ").toList.sortWith(_.length > _.length).headOption.getOrElse("unknown")

      val candidate_frags = candidate.split("[, ]+")

      val candidate_frag_scores = candidate_frags.map { frag =>
        frag -> FuzzySearch.weightedRatio(frag, longest_surname_fragment)
      }
      val best_frag_score = candidate_frag_scores.filter {
        case (frag, score) if score > min_surname_score && frag.length >= min_useful_name_len => true
        case _ => false
      }.sortWith(_._2 > _._2).headOption

      best_frag_score match {
        case Some((frag, _)) =>
          val overall_score = FuzzySearch.weightedRatio(candidate, box_name)
          if (overall_score >= min_overall_score)
            StrongSurnameMatch(box_name, overall_score)
          else
            WeakSurnameMatch(box_name, overall_score, s"[$candidate] vs [$box_name]: " +
              s"Matched [$longest_surname_fragment] with [$best_frag_score], but overall score was [$overall_score]"
            )
        case None => // We will record cases where there is a strong first name match
          //(note we don't bother recording the surname strength in this case since empirically they seem a bit random below 60ish)
          val candidate_frag_set = candidate_frags.toSet
          val box_first_names = box_name_decomp.toList.drop(1).headOption.map(_.split("[ ]+").toList)
          val maybe_exact_first_name = box_first_names.collect {
            case List(single_first_name) //needs to be strong enough and match
              if single_first_name.length >= min_useful_name_len && candidate_frag_set(single_first_name) => single_first_name
          }
          val maybe_near_first_name = box_first_names match {
            case _ if maybe_exact_first_name.nonEmpty => None
            case Some(List(single_first_name)) if single_first_name.length >= min_useful_name_len =>
              candidate_frags.filter(FuzzySearch.weightedRatio(_, single_first_name) >= min_first_name_score).headOption
          }
          NoSurnameMatch(box_name, maybe_exact_first_name, maybe_near_first_name, s"[$candidate] vs [$box_name]: " +
            s"Failed to find a fragment matching [$longest_surname_fragment], candidates=${candidate_frag_scores.mkString(";")}"
          )
      }
    }

    /** Only used to reduce diagnosis prints */
    var fixes_for_debug: Map[String, (String, String, Boolean)] = Map()

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
            println(s"$prefix: ERROR.X: [$result] vs [$curr_result] ([$context] vs [$curr_context]) (key=[$key])")
            fixes_for_debug = fixes_for_debug + (key -> (curr_result, context, true)) //(overwrite so only display this error once)
          case Some((`result`, _, _)) => // nothing to do
          case _ =>
            println(s"$prefix: $result (key=[$key])")
            fixes_for_debug = fixes_for_debug + (key -> (result, context, true))
        }
      }

      match_info match {

        // Options:
        // There is a single strong match (0+ weak matches): pick that
        // There is a single weak match: pick that
        // There are 0 weak matches, but the first name matches and is not similar to any strings occurring elsewhere

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
            val context_string = s"ERROR.2A: multiple first name matches: [$candidate] vs [$l]"
            log_info(None, context_string)
            Left(context_string)
          } else if (l2.exists(_.near_first_name.nonEmpty)) {
            // We have to do one final check: are there any _fuzzy_ matches to the first name
            // (we're being pretty cautious in our recklessness here!!)
            val bad_l2 = l2.filter(_.near_first_name.nonEmpty)
            val context_string = s"ERROR.2A: multiple near first name matches: [$candidate] vs [$bad_l2]"
            log_info(None, context_string)
            Left(context_string)
          } else {
            val context_string = s"SUCCESS.2C: 'first name only' match: [$first_name_only]"
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
