package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._

/** Wraps several functions for finding and fixing errors in the PbP/box score */
object LineupErrorAnalysisUtils {
  /** For debugging unknown cases */
  private val debug = true

  /** Possible ways in which a lineup can be declared invalid */
  object ValidationError extends Enumeration {
    val WrongNumberOfPlayers, UnknownPlayers = Value
  }

  case class TidyPlayerContext(
    all_players_map: Map[String, String],
    alt_all_players_map: Map[String, List[String]]
  )

  /** AaBbb... -> ABbbbbb, see below for explanation */
  private def truncate_code(code: String): String = {
    if ((code.length >= 3) && code(2).isUpper) { // AaBbb... -> ABbbbbb
      code(0) + code.drop(2)
    } else {
      code
    }
  }

  /** Builds some alternative maps of player codes vs player names */
  def build_tidy_player_context(box_lineup: LineupEvent): TidyPlayerContext = {
    val all_players_map = box_lineup.players.map(p => p.code -> p.id.name).toMap
    // Sometimes the game has SURNAME,INITIAL instead of SURNAME,NAME
    val alt_all_players_map = all_players_map.toList.groupBy { case (code, name) =>
      truncate_code(code)
    }.mapValues(_.map(pp => pp._2)) //ie code -> list(names)
    TidyPlayerContext(all_players_map, alt_all_players_map)
  }
  /** Grabs the tidy version of a player's name from the box score */
  def tidy_player(p: String, ctx: TidyPlayerContext): String = {
    val player_id = ExtractorUtils.build_player_code(p)
    ctx.all_players_map
      .get(player_id.code)
      .orElse { // See if it's the alternative
        ctx.alt_all_players_map.get(player_id.code).collect {
          case unique :: Nil => unique
        }
      }.orElse { // Sometimes the first half of a double barrrel is missed
        var new_p = p.replaceAll("[a-zA-Z]+-([a-zA-Z]+)", "$1")
        if (new_p != p) {
          Some(tidy_player(new_p, ctx))
        } else None
      }.orElse {
        // Often we see "D.J."" in the box lineup, and then (say) "Dan" in the PBP
        val truncated_code = truncate_code(player_id.code)
        ctx.alt_all_players_map.get(truncated_code).flatMap {
          case unique :: Nil =>
            // Insert a "j" for junior into the name and see if that fixes it:
            val junior = truncated_code(0) + "j" + truncated_code.drop(1)
            ctx.all_players_map.get(junior)

          case _ => None
        }

      }.getOrElse(p) //(this will get rejected later on, in validate_lineup)
  }

  /** Pulls out inconsistent lineups (self healing seems harder
    * based on cases I've seen, eg
    * player B enters game+player A leaves game ...
    * ... A makes shot...player A enters game)
   */
  def validate_lineup(
    lineup_event: LineupEvent, valid_player_codes: Set[String]
  ): Set[ValidationError.Value] = {
    // Check the right number of players:
    val right_number_of_players = lineup_event.players.size == 5
    // We also see cases where players not mentioned in the box score make plays
    // (I think this is normally when a player is missing from the box score for some reason?
    //  but formatting errors are also possible?)
    val all_players_known = lineup_event.players.forall {
      case LineupEvent.PlayerCodeId(code, _) => valid_player_codes(code)
    }
    //TODO: what about if a player not in the game is mentioned in a game event?

    Set(ValidationError.WrongNumberOfPlayers).filterNot(_ => right_number_of_players) ++
    Set(ValidationError.UnknownPlayers).filterNot(_ => all_players_known) ++
    Set() // (terminator)
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
    clump: BadLineupClump, valid_player_codes: Set[String]
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
        if (validate_lineup(fixed_lineup_ev, valid_player_codes).isEmpty) {
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
              ExtractorUtils.build_player_code(tidy_player(player, tidy_ctx))
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
        }.partition(validate_lineup(_, valid_player_codes).isEmpty)

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
              ExtractorUtils.build_player_code(tidy_player(player, tidy_ctx))
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
        }.partition(validate_lineup(_, valid_player_codes).isEmpty)

        (good_lineups, BadLineupClump(bad_lineups, clump.next_good))
      } else {
        (Nil, clump)
      }
    }
  }

  /** Prints out the info for unfixed clumps */
  def analyze_unfixed_clumps(
    clump: BadLineupClump, valid_player_codes: Set[String]
  ) = {
    val debug_prefix = "__auc__"

    // The set of candidate players:
    val candidates = clump.evs.headOption.map(_.players).getOrElse(Nil).toSet
    val expected_size_diff = candidates.size - 5

    println(s"$debug_prefix --------------------------------------------------")
    println(s"$debug_prefix CLUMP [+$expected_size_diff] size=[${clump.evs.size}] [?${clump.next_good.nonEmpty}] [!${clump.evs.headOption.map(ev => validate_lineup(ev, valid_player_codes))}]")
    println(s"$debug_prefix valid player codes: [$valid_player_codes]")

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
      handle_common_sub_bug(to_fix, valid_player_codes)
    }.map { case (fixed, to_fix) =>
      val (newly_fixed, still_to_fix) = find_missing_subs(to_fix, box_lineup, valid_player_codes)
      (fixed ++ newly_fixed, still_to_fix)
    }.map { case (fixed, to_fix) =>
      val (newly_fixed, still_to_fix) = add_missing_players(to_fix, box_lineup, valid_player_codes)
      (fixed ++ newly_fixed, still_to_fix)
    }.map { case (fixed, to_fix) =>
      if (debug && to_fix.evs.nonEmpty) { //(if debug flag enabled, display some info about unfixed clumps)
        analyze_unfixed_clumps(to_fix, valid_player_codes)
      }
      (fixed, to_fix)
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
}
