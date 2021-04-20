package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._

import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult

/** Wraps several functions for finding and fixing errors in the PbP/box score */
object LineupErrorAnalysisUtils {
  /** For debugging unknown cases */
  private val debug = true

  /** Possible ways in which a lineup can be declared invalid */
  object ValidationError extends Enumeration {
    val WrongNumberOfPlayers, UnknownPlayers, InactivePlayers = Value
  }
  val allowedErrors: Set[ValidationError.Value] = (
    ValidationError.WrongNumberOfPlayers :: // <>5 players in lineup
    ValidationError.UnknownPlayers :: // players not in the box score
    ValidationError.InactivePlayers :: // players mentioned in game events who aren't in the lineup
    Nil
  ).toSet


  /** Context for tidying players, includes a cache for performance if you want to fold with it*/
  case class TidyPlayerContext(
    box_lineup: LineupEvent,
    all_players_map: Map[String, String],
    alt_all_players_map: Map[String, List[String]],
    resolution_cache: Map[String, String] = Map()
  )

  /** AaBbb... -> ABbbbbb, see below for explanation */
  private def truncate_code_1(code: String): String = {
    val truncation_regex = "^([A-Z]).*?([A-Z][a-z.-]*)$".r //(initial then last name)
    code match {
      case truncation_regex(initial, surname) => initial + surname
      case _ => code
    }
  }
  /** AaBbCccc... -> AaCccc, see below for explanation */
  private def truncate_code_2(code: String): String = {
    val truncation_regex = "^([A-Z][a-z]).*?([A-Z][a-z.-]*)$".r //(initial then last name)
    code match {
      case truncation_regex(first_name, surname) => first_name + surname
      case _ => code
    }
  }

  /** Builds some alternative maps of player codes vs player names */
  def build_tidy_player_context(box_lineup: LineupEvent): TidyPlayerContext = {
    val all_players_map = box_lineup.players.map(p => p.code -> p.id.name).toMap
    // Sometimes the game event has SURNAME,INITIAL instead of SURNAME,NAME
    // And sometimes SURNAME,NAME1 instead of SURNAME,NAME1 NAME2
    val alt_all_players_map = all_players_map.toList.groupBy { case (code, name) =>
      truncate_code_1(code)
    }.mapValues(_.map(pp => pp._2)) ++ all_players_map.toList.groupBy { case (code, name) =>
      truncate_code_2(code)
    }.mapValues(_.map(pp => pp._2)) //ie code -> list(names)
    TidyPlayerContext(box_lineup, all_players_map, alt_all_players_map)
  }

  /** Grabs the tidy version of a player's name from the box score */
  def tidy_player(p_in: String, ctx: TidyPlayerContext): (String, TidyPlayerContext) = {
    ctx.resolution_cache.get(p_in).map((_, ctx)).getOrElse {
      val player_id = ExtractorUtils.build_player_code(p_in, Some(ctx.box_lineup.team.team))
      val p = player_id.id.name //(normalized name)
      def with_updated_cache(resolved_p: String) = {
        ctx.copy(resolution_cache = ctx.resolution_cache + (p -> resolved_p))
      }

      ctx.all_players_map
        .get(player_id.code)
        .orElse { // See if it's the alternative
          ctx.alt_all_players_map.get(player_id.code).collect {
            case unique :: Nil => unique
          }
        }.orElse { // Sometimes the first half of a double barrrel is missed
          var new_p = p.replaceAll("[a-zA-Z]+-([a-zA-Z]+)", "$1")
          if (new_p != p) {
            Some(tidy_player(new_p, ctx)._1)
          } else None
        }.orElse { // Is it initials?
          convert_from_initials(p, ctx.all_players_map)
        }.orElse { // Is it a number?
          convert_from_digits(p, ctx.box_lineup.players_out)
        }.orElse {
          // Often we see "D.J."" in the box lineup, and then (say) "Dan" in the PBP
          val truncated_code = truncate_code_1(player_id.code)
          ctx.alt_all_players_map.get(truncated_code).flatMap {
            case unique :: Nil =>
              // Insert a "j" for junior into the name and see if that fixes it:
              val junior = truncated_code(0) + "j" + truncated_code.drop(1)
              ctx.all_players_map.get(junior)

            case _ => None
          }

        }.orElse { // OK if we're done to here, let's get creative:
          if (p != "Team" && p != "TEAM" && p != "TEAM DEF" && p != "TEAM FULL")
          {
            //^ (various team stats, we exclude them just so we don't accidentally fuzzy match on a player...)
            NameFixer.fuzzy_box_match(
              p, ctx.all_players_map.values.toList, s"${ctx.box_lineup.team}"
            ) match {
              case Right(box_name) => Some(box_name)
              case Left(_) => None
            }
          } else { // just normalize all the team mentions so at least we catch them in one place (validate_lineup)
            Some("Team")
          }
        }.map { resolved_p =>
          (resolved_p, with_updated_cache(resolved_p))
        }.getOrElse((p, with_updated_cache(p))) //(this will get rejected later on, in validate_lineup)
    }
  }

  /** Find a match for a set of initials */
  def convert_from_initials(name: String, codes_to_names: Map[String, String]): Option[String] = {
    ExtractorUtils.name_is_initials(name) match {
      case Some((p1, p2)) =>
        val candidates = codes_to_names.collect {
          case (code, id) if (code.size >= 3) && (code(0) == p1) && (code(2) == p2) => id
        }
        candidates match {
          case List(single_match) => Some(single_match)
          case _ => None
        }
      case _ => None
    }
  }

  def convert_from_digits(name: String, player_numbers: List[LineupEvent.PlayerCodeId]): Option[String] = {
    if (name.forall(_.isDigit)) {
      player_numbers.find(_.code == name).map(_.id.name)
    } else {
      None
    }
  }

  /** Pulls out inconsistent lineups (self healing seems harder
    * based on cases I've seen, eg
    * player B enters game+player A leaves game ...
    * ... A makes shot...player A enters game)
   */
  def validate_lineup(
    lineup_event: LineupEvent, box_lineup: LineupEvent, valid_player_codes: Set[String]
  ): Set[ValidationError.Value] = {
    // Check the right number of players:
    val right_number_of_players = lineup_event.players.size == 5
    // We also see cases where players not mentioned in the box score make plays
    // (I think this is normally when a player is missing from the box score for some reason?
    //  but formatting errors are also possible?)
    val all_players_known = lineup_event.players.forall {
      case LineupEvent.PlayerCodeId(code, _) => valid_player_codes(code)
    }
    // Players not mentioned in game
    val tidy_ctx = build_tidy_player_context(box_lineup)
    //TODO: move this to a private fn since it's used in a few places
    val inactive_players_mentioned = lineup_event.raw_game_events.flatMap(_.team.toList).collect {
      case EventUtils.ParseAnyPlay(player) if player.toLowerCase != "team" =>
        ExtractorUtils.build_player_code(tidy_player(player, tidy_ctx)._1, Some(lineup_event.team.team)).code
    }.filterNot(valid_player_codes)

    (Set(ValidationError.WrongNumberOfPlayers).filterNot(_ => right_number_of_players) ++
    Set(ValidationError.UnknownPlayers).filterNot(_ => all_players_known) ++
    Set(ValidationError.InactivePlayers).filter(_ => inactive_players_mentioned.nonEmpty) ++
    Set()).filter(allowedErrors) // (terminator)
  }

  /** Wraps a related clump of bad lineup events, plus the first following good event */
  protected case class BadLineupClump(
    evs: List[LineupEvent], next_good: Option[LineupEvent] = None
  )

  /** Clump concurrent bad lineups TODO test */
  def clump_bad_lineups(
    lineup_events: List[(LineupEvent, Option[LineupEvent])]
  ): List[BadLineupClump] =
  {
    lineup_events.foldLeft(List[BadLineupClump]()) {
      case (Nil, (lineup, next)) => List(BadLineupClump(lineup :: Nil, next))
      case (
        clumps @ (BadLineupClump(clump_evs @ (last :: lineup_tail), _) :: clump_tail),
        (lineup, next)
      ) =>
        if (
          (lineup.team == last.team) &&
          (lineup.opponent == last.opponent) &&
          (lineup.start_min == last.end_min) &&
          (lineup.players.size == last.players.size) &&
          //(this is an interesting one - once you get a sub mismatch it becomes a bit hard to reason)
          (lineup.players_in.size == lineup.players_out.size)
        )
        {
          // Add to clump
          BadLineupClump(lineup :: clump_evs, next) :: clump_tail
        } else { // New clump
          BadLineupClump(lineup :: Nil, next) :: clumps
        }
      case (other, (lineup, next)) => BadLineupClump(lineup :: Nil, next) :: other //(not possible in practice)
    }.map(clump => clump.copy(evs = clump.evs.reverse)).reverse
  }

  /** eg 2 in 1 out followed by 1 out next lineup
   * Handles the cases
   * "IN: X, Y, Z; OUT: A, B" ... "OUT: C" and similarly
   * "IN: X, Y, Z; OUT: A" ... "OUT: C, Z"
  */
  def handle_common_sub_bug(
    clump: BadLineupClump, box_lineup: LineupEvent, valid_player_codes: Set[String]
  ): (List[LineupEvent], BadLineupClump) = {
    clump match {
      case BadLineupClump(bad :: Nil, Some(good))
        if (bad.players_in.size > bad.players_out.size) &&
            (good.players_in.size == 0) && //(else can't tell which good.player_out to use)
            (good.players_out.size > 0)
      =>
        val all_players = bad.players.filterNot(good.players_out.toSet)
        val fixed_lineup_ev = bad.copy(
          players_out = (bad.players_out ++ good.players_out).distinct,
          players = all_players
        )
        if (validate_lineup(fixed_lineup_ev, box_lineup, valid_player_codes).isEmpty) {
          (fixed_lineup_ev :: Nil, BadLineupClump(Nil, None))
        } else {
          (Nil, BadLineupClump(fixed_lineup_ev :: Nil, Some(good)))
        }

      case _ => (Nil, clump)
    }
  }

  /** Debug print for displaying lineup events */
  private def display_lineup(ev: LineupEvent, prefix: String) {
    println(s"$prefix [${ev.team.team}] vs [${ev.opponent.team}] ([${ev.location_type}])")
    println(s"$prefix IN: ${ev.players_in.map(_.code).mkString(",")}")
    println(s"$prefix OUT: ${ev.players_out.map(_.code).mkString(",")}")
    println(s"$prefix ON: ${ev.players.map(_.code).mkString(",")}")
    println(s"$prefix RAW:\n$prefix ${ev.raw_game_events.flatMap(_.team).mkString(s"\n$prefix ")}")
    println(s"$prefix -----------")
  }

  /** If there are fewer than 5 players, find some to add */
  protected def add_missing_players(
    clump: BadLineupClump, box_lineup: LineupEvent, valid_player_codes: Set[String]
  ): (List[LineupEvent], BadLineupClump) = {
    // For trace level debugging
    val extra_debug = false
    val debug_prefix = "__amp__"

    val players_in = clump.evs.headOption.map(_.players).getOrElse(Nil).toSet
    if (players_in.size > 4) { // Nothing to do
      (Nil, clump)
    } else {
      val candidates = box_lineup.players.toSet.filterNot(players_in)

      // The set of candidate players:
      val expected_size_diff = 5 - players_in.size
      val tidy_ctx = build_tidy_player_context(box_lineup)

      if (extra_debug && debug) {
        println(s"$debug_prefix --------------------------------------------------")
        println(s"$debug_prefix CLUMP [-$expected_size_diff] size=[${clump.evs.size}] [?${clump.next_good.nonEmpty}]")
      }
      val related_evs = clump.evs //(ie don't include next good)
      case class State(
        curr_candidates: Set[LineupEvent.PlayerCodeId],
        found_players: Set[LineupEvent.PlayerCodeId],
        last_event: Option[LineupEvent]
      )
      val State(_, players_to_add, _) = related_evs.foldLeft(State(candidates, Set(), None)) {
        case (State(curr_candidates, curr_to_add, _), ev) =>
          // If you get subbed in, you cease to become a candidate
          val new_candidates = curr_candidates -- ev.players_in.filter(curr_candidates)
          // Any candidates who are mentioned get added to the list:
          val candidates_who_are_in_plays = ev.raw_game_events.flatMap(_.team.toList).collect {
            case EventUtils.ParseAnyPlay(player) =>
              ExtractorUtils.build_player_code(tidy_player(player, tidy_ctx)._1, Some(ev.team.team))
          }.filter(new_candidates)

          if (extra_debug && debug) {
            display_lineup(ev, debug_prefix)
            println(s"$debug_prefix candidates=[${curr_candidates.map(_.code)}] > [${new_candidates.map(_.code)}]: found [${candidates_who_are_in_plays.map(_.code)}]")
          }

          State(new_candidates, curr_to_add ++ candidates_who_are_in_plays, Some(ev))
      }
      if (players_to_add.nonEmpty) {
        val (good_lineups, bad_lineups) = clump.evs.map { ev =>
          ev.copy(
            players = ev.players ++ players_to_add
          )
        }.partition(validate_lineup(_, box_lineup, valid_player_codes).isEmpty)

        (good_lineups, BadLineupClump(bad_lineups, clump.next_good))
      } else {
        (Nil, clump)
      }
    }
  }

  /** Goes over the events in a clump removing players who could not
   *  be the missing sub
   */
  protected def find_missing_subs(
    clump: BadLineupClump, box_lineup: LineupEvent, valid_player_codes: Set[String]
  ): (List[LineupEvent], BadLineupClump) = {
    // For trace level debugging
    val extra_debug = false
    val debug_prefix = "__fms__"

    val candidates = clump.evs.headOption.map(_.players).getOrElse(Nil).toSet
    if (candidates.size < 6) { // Nothing to do
      (Nil, clump)
    } else {
      val expected_size_diff = candidates.size - 5
      val tidy_ctx = build_tidy_player_context(box_lineup)

      if (extra_debug && debug) {
        println(s"$debug_prefix --------------------------------------------------")
        println(s"$debug_prefix CLUMP [+$expected_size_diff] size=[${clump.evs.size}] [?${clump.next_good.nonEmpty}]")
      }

      val related_evs = clump.evs //(ie don't include next good)
      case class State(curr_candidates: Set[LineupEvent.PlayerCodeId], last_event: Option[LineupEvent])
      val State(filtered_candidates, _) = related_evs.foldLeft(State(candidates, None)) {
        case (State(curr_candidates, _), ev) =>
          val candidates_who_sub_out =
            if (clump.evs.headOption.contains(ev)) Set() else ev.players_out.filter(curr_candidates)
            //(for the first element, only look at which players are involved in PbP)
          val candidates_who_are_in_plays = ev.raw_game_events.flatMap(_.team.toList).collect {
            case EventUtils.ParseAnyPlay(player) =>
              ExtractorUtils.build_player_code(tidy_player(player, tidy_ctx)._1, Some(ev.team.team))
          }.filter(curr_candidates)

          if (extra_debug && debug) {
            display_lineup(ev, debug_prefix)
            println(s"$debug_prefix candidates=[${curr_candidates.map(_.code)}] - [${candidates_who_sub_out.map(_.code)}] - [${candidates_who_are_in_plays.map(_.code)}]")
          }

          State(
            curr_candidates -- candidates_who_sub_out -- candidates_who_are_in_plays,
            Some(ev)
          )
      }
      if (filtered_candidates.nonEmpty && (filtered_candidates.size <= expected_size_diff)) {
        val (good_lineups, bad_lineups) = clump.evs.map { ev =>
          ev.copy(
            players = ev.players.filterNot(filtered_candidates)
          )
        }.partition(validate_lineup(_, box_lineup, valid_player_codes).isEmpty)

        (good_lineups, BadLineupClump(bad_lineups, clump.next_good))
      } else {
        (Nil, clump)
      }
    }
  }

  /** Prints out the info for unfixed clumps */
  def analyze_unfixed_clumps(
    clump: BadLineupClump, box_lineup: LineupEvent, valid_player_codes: Set[String]
  ) = {
    val debug_prefix = "__auc__"

    // The set of candidate players:
    val candidates = clump.evs.headOption.map(_.players).getOrElse(Nil).toSet
    val expected_size_diff = candidates.size - 5

    val validation = clump.evs.headOption.map(ev => validate_lineup(ev, box_lineup, valid_player_codes))

    println(s"$debug_prefix --------------------------------------------------")
    println(s"$debug_prefix CLUMP [+$expected_size_diff] size=[${clump.evs.size}] [?${clump.next_good.nonEmpty}] [!${validation}]")
    println(s"$debug_prefix valid player codes: [$valid_player_codes]")

    if (validation.exists(_(ValidationError.UnknownPlayers))) {
      val all_unknown = clump.evs.flatMap(_.players).filterNot(id_code => valid_player_codes(id_code.code)).distinct
      println(s"$debug_prefix UNKNOWN_PBP [$all_unknown]")
      println(s"$debug_prefix UNKNOWN_BOX [${box_lineup.players}]")
    }

    println(s"$debug_prefix Unfixed\n${clump.evs.foreach(ev => display_lineup(ev, debug_prefix))}")
    clump.next_good.foreach(ev => println(s"$debug_prefix (next good)\n${display_lineup(ev, debug_prefix)}"))
  }

  /** Fixes certain cases where the lineup is corrupt */
  def analyze_and_fix_clumps(
    clump: BadLineupClump, box_lineup: LineupEvent, valid_player_codes: Set[String]
  ): (List[LineupEvent], BadLineupClump) = {

    //TODO: other ideas
    // 1) If a player who is supposed to be out and is subbed out (or vice versa)
    // 3) Look for lineups where all 5 players are mentioned:
    // (see https://github.com/Alex-At-Home/cbb-explorer/issues/6#issuecomment-560131070)
    // 4) Use minutes distributions
    // 5) Use preceding clump's info:
    // (see https://github.com/Alex-At-Home/cbb-explorer/issues/6#issuecomment-560132341)

    Some(clump).map { to_fix =>
      handle_common_sub_bug(to_fix, box_lineup, valid_player_codes)
    }.map { case (fixed, to_fix) =>
      val (newly_fixed, still_to_fix) = find_missing_subs(to_fix, box_lineup, valid_player_codes)
      (fixed ++ newly_fixed, still_to_fix)
    }.map { case (fixed, to_fix) =>
      val (newly_fixed, still_to_fix) = add_missing_players(to_fix, box_lineup, valid_player_codes)
      (fixed ++ newly_fixed, still_to_fix)
    }.map { case (fixed, to_fix) =>
      if (debug && to_fix.evs.nonEmpty) { //(if debug flag enabled, display some info about unfixed clumps)
        analyze_unfixed_clumps(to_fix, box_lineup, valid_player_codes)
      }
      (fixed.map { fixed_lineup => //recalculate the lineup id now that I've removed players
        fixed_lineup.copy(lineup_id = ExtractorUtils.build_lineup_id(fixed_lineup.players))
      }, to_fix)
    }.getOrElse {
      (Nil, clump)
    }
  }

  /** Given a list of bad lineups, categorizes them into a map of the number of players
      (5 indicates that there was a wrong player in there)
      With the values being the number of "clumps" and the total possessions
      * FOR DISPLAY ONLY (can live without tests)
      */
  def categorize_bad_lineups(
    lineup_events: List[LineupEvent]
  ): Map[Int, (Int, Int)] = {
    val bad_clumps = clump_bad_lineups(lineup_events.map(e => (e, None)))
    bad_clumps.groupBy(_.evs.headOption.map(_.players.size).getOrElse(0)).mapValues { clumps =>
      (clumps.size, clumps.foldLeft(0) { (acc, clump) =>
        acc + clump.evs.foldLeft(0) { (sub_acc, lineup) => sub_acc + lineup.team_stats.num_possessions }
      })
    }
  }

  object NameFixer {

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

      // First name analysis (only do it if we have to)
      def decompose_first_names(ignore: Option[String] = None): (Option[String], Option[String]) = {
        val filtered_frags = candidate_frags.filterNot(Some(_) == ignore)
        val candidate_frag_set = filtered_frags.toSet
        val box_first_names = box_name_decomp.toList.drop(1).headOption.map(_.split("[, ]+").toList.filter(remove_jr))
        val maybe_exact_first_name = box_first_names.collect {
          case List(single_first_name) //needs to be strong enough and match
            if single_first_name.length >= min_useful_first_name_len && candidate_frag_set(single_first_name) => single_first_name
        }
        val maybe_near_first_name = box_first_names match {
          case _ if maybe_exact_first_name.nonEmpty => None
          case Some(List(single_first_name)) if single_first_name.length >= min_useful_first_name_len =>
            filtered_frags.filter(FuzzySearch.weightedRatio(_, single_first_name) >= min_first_name_score).headOption
          case _ => None
        }
        (maybe_exact_first_name, maybe_near_first_name)
      }

      // Surname analysis

      val candidate_frag_scores = candidate_frags.map { frag =>
        frag -> FuzzySearch.weightedRatio(frag, longest_surname_fragment)
      }
      val best_frag_score = candidate_frag_scores.filter {
        case (frag, _) if (longest_surname_fragment == frag) && (frag.length >= (min_useful_surname_len - 1)) => true
          //(more permissive if they are actually ==)
        case (frag, score) if (score > min_surname_score) && (frag.length >= min_useful_surname_len) => true

        case (frag, score) if (score > min_surname_score) && (frag.length >= min_useful_surname_len - 1) =>
          //(also more permissive if the first names match)
          val (maybe_exact_first_name, maybe_near_first_name) = decompose_first_names(Some(frag));
          maybe_exact_first_name.nonEmpty && maybe_near_first_name.isEmpty

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
          val (maybe_exact_first_name, maybe_near_first_name) = decompose_first_names();
          //(note we don't bother recording the surname strength in this case since empirically they seem a bit random below 60ish)
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
        val prefix = "LineupErrorAnalysisUtils.NameFixer"
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

                if (new_candidates.nonEmpty) {
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
