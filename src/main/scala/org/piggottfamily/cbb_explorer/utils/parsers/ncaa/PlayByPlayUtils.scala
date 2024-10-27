package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._
import cats.implicits._
import cats.data._
import com.github.dwickern.macros.NameOf._
import shapeless._
import ops.hlist._
import shapeless.labelled._
import record._
import ops.record._
import syntax.singleton._
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.Try
import scala.annotation.tailrec

/** Utils to enrich other data using PbP events */
trait PlayByPlayUtils {
  import ExtractorUtils._

  /** Appends lineup ids, shot type, etc to the shot events using PBP/lineup
    * event info
    */
  def enrich_shot_events_with_pbp(
      sorted_shot_events: List[ShotEvent],
      sorted_pbp_events: List[Model.PlayByPlayEvent],
      lineup_events: List[LineupEvent],
      bad_lineup_events: List[LineupEvent],
      box_lineup: LineupEvent
  ): List[ShotEvent] = {
    import ShotEnrichmentUtils._

    val tidy_ctx =
      LineupErrorAnalysisUtils.build_tidy_player_context(box_lineup)

    val startingState = EnrichmentState(
      pbp_it = sorted_pbp_events.iterator,
      lineup_it = lineup_events.iterator
    )
    val finishingState = sorted_shot_events.foldLeft(startingState) {
      case (state, shot) =>
        val (pbp_clump, maybe_next_pbp_event) =
          find_pbp_clump(
            shot.shot_min,
            state.pbp_it,
            state.curr_pbp_clump,
            state.maybe_next_pbp_event
          )

        // (Useful debug print in conjunction with an if clause)
        // println(
        //   s"PBP ANALYSIS [$pbp_clump][$maybe_next_pbp_event] <- [${state.curr_pbp_clump}]"
        // )

        val (maybe_enriched_shot, remaining_pbp_events, saved_lineups) =
          (
            pbp_clump
              .collect { // (ignore game events from the other team than took the shot)
                case ev: Model.OtherTeamEvent if shot.is_off      => ev
                case ev: Model.OtherOpponentEvent if !shot.is_off => ev
              }
          ) match {
            case pbp_clump_for_shot if pbp_clump_for_shot.nonEmpty =>
              val (pbp_shots, pbp_assists) =
                pbp_clump_for_shot.partition(ev =>
                  shot_value(ev.event_string) > 0
                )

              val maybe_selected_pbp =
                pbp_shots.filter(ev => right_kind_of_shot(shot, ev)) match {
                  case Nil => None
                  case candidate_matches =>
                    val player_filtered_candidate_matches =
                      candidate_matches.filter(ev => {
                        matching_player(shot, ev, tidy_ctx, code_match = false)
                      }) match {
                        case Nil => // retry but with less strict matching
                          candidate_matches.filter(ev => {
                            matching_player(
                              shot,
                              ev,
                              tidy_ctx,
                              code_match = true
                            )
                          })
                        case matches => matches
                      }
                    player_filtered_candidate_matches match {
                      case Nil if candidate_matches.size == 1 =>
                        // only one candidate match, so just use it
                        // Warning since this PbP event must match this shot, but the player codes don't match up
                        println(
                          s"[enrich_shot_events_with_pbp] WARN: IGNORED player mismatch for shot [$shot] vs [$candidate_matches]"
                        )
                        candidate_matches.headOption
                      case Nil =>
                        // Too many "wrong" candidate matches (or none, eg all the options were wrong distance/direction), bailing
                        println(
                          s"[enrich_shot_events_with_pbp] WARN: discarding unmatched shot [$shot], candidates=[$candidate_matches]"
                        )
                        None
                      case other => // Pick first, assume ordering is correct!
                        player_filtered_candidate_matches.headOption
                    }
                }

              maybe_selected_pbp match {
                case Some(selected_pbp) =>
                  // We have a matching PbP event, now match it to a lineup:
                  val ((curr_lineup, stashed_lineups), used_bad_lineup) =
                    find_lineup(
                      shot,
                      maybe_selected_pbp,
                      state.curr_lineups,
                      state.lineup_it
                    ) match {
                      case (None, saved_stashed_lineups) =>
                        // Try with bad lineups:
                        // (Don't care that this is inefficient, it's a rare case)
                        val (curr_bad_lineup, _) = find_lineup(
                          shot,
                          maybe_selected_pbp,
                          Nil,
                          bad_lineup_events.iterator
                        )

                        val debug_used_lineups = false
                        if (debug_used_lineups) {
                          println(s"NO LINEUPS IN [${state.curr_lineups
                              .map(l => f"[${l.start_min}%.2f->${l.end_min}%.2f]")}]: STASHED [${saved_stashed_lineups
                              .map(l => f"[${l.start_min}%.2f->${l.end_min}%.2f]")}]")
                        }
                        ((curr_bad_lineup, saved_stashed_lineups), true)

                      case other => (other, false)
                    }
                  curr_lineup match {
                    case Some(lineup) =>
                      // Look for assists:
                      val maybe_assist_pbp =
                        pbp_assists
                          .filter(_ =>
                            shot.pts > 0
                          ) // (can't assist a missed shot)
                          .find(ev =>
                            !matching_player(
                              shot,
                              ev,
                              tidy_ctx,
                              code_match = true
                            )
                          ) // (can't self-assist!)

                      val shot_val = shot_value(selected_pbp.event_string)
                      val enriched_shot = shot.copy(
                        shooter = shot.shooter.filter(_ =>
                          shot.is_off
                        ), // (discard oppo shooters)
                        lineup_id = Some(lineup.lineup_id).filterNot(_ =>
                          used_bad_lineup
                        ),
                        raw_event = None,
                        players = lineup.players,
                        pts = shot.pts * shot_val,
                        value = shot_val,
                        is_assisted = Some(maybe_assist_pbp.isDefined)
                          .filter(_ == true),
                        assisted_by = maybe_assist_pbp
                          .filter(_ => shot.is_off) // (discard oppo passers)
                          .flatMap(ev =>
                            extract_player_from_ev(shot, ev, tidy_ctx)
                          ),
                        in_transition = Some(
                          selected_pbp.event_string.contains("fastbreak")
                        ).filter(_ == true)
                      )

                      val pbp_clump_minus_matching_events =
                        pbp_clump.filterNot(ev =>
                          (ev eq selected_pbp) || maybe_assist_pbp.exists(
                            _ eq ev
                          )
                        )

                      maybe_debug_event(enriched_shot)
                      (
                        Some(enriched_shot),
                        pbp_clump_minus_matching_events,
                        stashed_lineups
                      )
                    case None => // No matching lineup
                      println(
                        s"[enrich_shot_events_with_pbp] WARN: discarding unmatched shot [$shot]([$selected_pbp]), NO_LINEUP ([$stashed_lineups])"
                      )
                      // For debugging: list all the lineup times
                      val no_lineup_debug = false
                      if (no_lineup_debug) {
                        val lineup_times = lineup_events
                          .map { lineup =>
                            f"[${lineup.start_min}%.2f->${lineup.end_min}%.2f]"
                          }
                        println(s"NO_LINEUP: $lineup_times")
                      }

                      (None, pbp_clump, stashed_lineups)
                  }
                case None =>
                  // (already added a warning for this case above)
                  (None, pbp_clump, state.curr_lineups)
              }
            case _ => // No matching PbP events
              println(
                s"[enrich_shot_events_with_pbp] WARN: discarding unmatched shot [$shot], NO_PBP ([$maybe_next_pbp_event])"
              )
              // For debugging: list all the PbP times
              val no_pbp_debug = false
              if (no_pbp_debug) {
                val pbp_times = sorted_pbp_events
                  .map {
                    case ev: Model.MiscGameEvent =>
                      if (Math.abs(ev.min - shot.shot_min) < 0.1) {
                        s"${ev.min}:[${ev.event_string}]"
                      } else f"${ev.min}%.2f"
                    case ev => f"(${ev.min}%.2f)"
                  }
                  .mkString(",")
                println(s"NO_PBP: $pbp_times")
              }

              (None, pbp_clump, state.curr_lineups)
          }
        state.copy(
          enriched_shot_events =
            state.enriched_shot_events ++ maybe_enriched_shot.toList,
          curr_pbp_clump = remaining_pbp_events,
          maybe_next_pbp_event = maybe_next_pbp_event,
          curr_lineups = saved_lineups
        )
    }
    finishingState.enriched_shot_events
  }

  ///////////////////// Utils for enrich_shot_events_with_pbp

  /** Separated out fiddly details for testing purposes */
  protected object ShotEnrichmentUtils {

    /** We always print out warnings but if true we also print out a summary of
      * the info
      */
    val debug_print_enriched = false

    def maybe_debug_event = (shot: ShotEvent) => {
      if (debug_print_enriched) {
        val team = shot.team.team.name match {
          case target_team if shot.is_off =>
            target_team.toUpperCase
          case oppo_team => oppo_team
        }
        val oppo = shot.opponent.team.name match {
          case target_team if !shot.is_off =>
            target_team.toUpperCase
          case oppo_team => oppo_team
        }
        val time = f"[${shot.shot_min}%.1f]"
        val shooter = shot.shooter.map(_.code) match {
          case Some(shooter) => shooter
          case _ if shot.is_off =>
            "ERROR"
          case _ => // opponents are removed
            "(oppo)"
        }
        val maybe_assisted_by =
          if (shot.is_off)
            shot.assisted_by.map(_.code).map(a => s"(A)[$a]").getOrElse("")
          else if (shot.is_assisted.contains(true)) "(A)"
          else ""

        val loc_info = f"[${shot.x}%.1f],[${shot.y}%.1f]=>[${shot.dist}%.1f]ft"
        val score_info = s"([${shot.score.scored}]-[${shot.score.allowed}])"
        val maybe_trans =
          if (shot.in_transition.contains(true)) "(TRANS)" else ""
        val lineup_info = shot.players.map(_.code).mkString("_")
        val shot_value =
          s"[${shot.value}]pts ${if (shot.pts > 0) "[MADE]" else "[MISSED]"}"

        println(
          s"[enrich_shot_events_with_pbp] $time$score_info $shot_value [$shooter]$maybe_assisted_by$maybe_trans $loc_info" +
            s" | [$team][$oppo] [$lineup_info]"
        )
      }
    }

    /** State object for matching up PbP events with shots */
    case class EnrichmentState(
        enriched_shot_events: List[ShotEvent] = Nil,
        pbp_it: Iterator[Model.PlayByPlayEvent],
        curr_pbp_clump: List[Model.MiscGameEvent] = Nil,
        maybe_next_pbp_event: Option[Model.MiscGameEvent] = None,
        lineup_it: Iterator[LineupEvent],
        curr_lineups: List[LineupEvent] = Nil
    )

    /** Finds the lineup event that encompasses the shot (true/false if it
      * matches), plus lineups that have been pulled from the iterator but are
      * available to be matches for future events
      */
    def find_lineup(
        shot: ShotEvent,
        curr_pbp: Option[Model.MiscGameEvent],
        /** Any lineups that might match for this event which have been pulled
          * from the iterator
          */
        curr_lineups: List[LineupEvent],
        lineup_it: Iterator[LineupEvent]
    ): (Option[LineupEvent], List[LineupEvent]) = {
      def lineup_matcher(shot_min: Double): LineupEvent => Boolean = { ev =>
        ev.end_min >= shot_min && shot_min >= ev.start_min
      }

      case class RecursionState(
          curr_lineup: Option[LineupEvent],
          fallback_lineups: List[LineupEvent],
          stashed_lineups: List[LineupEvent]
      )

      @tailrec
      def find_lineup_recurse(
          recursion_state: RecursionState
      ): RecursionState = recursion_state match {
        case RecursionState(
              curr_lineup,
              fallback_lineups,
              stashed_lineups
            ) =>
          // Step 1: get a lineup that matches the time
          val stashed_it = stashed_lineups.iterator
          val maybe_matching_lineup = curr_lineup match {
            case Some(lineup)
                if lineup_matcher(
                  shot.shot_min
                )(lineup) =>
              curr_lineup
            case _ =>
              // Check stash then back to main list looking for candidate lineups
              stashed_it
                .find(shot.shot_min <= _.end_min)
                .orElse(
                  lineup_it.find(shot.shot_min <= _.end_min)
                )
          }
          val updated_stash =
            stashed_it.toList // (keep any lineups we haven't stepped into yet

          // Step 2:
          // If we have a matching element, we need to handle the special case where
          // the shot time is exactly the end of the lineup - does it go in this lineup or
          // the next one (2.4), plus misc other cases (2.1 - 2.3)
          (maybe_matching_lineup, fallback_lineups) match {
            case (None, _) =>
              // 2.1] (no more data in main it, just return fallback)
              RecursionState(
                curr_lineup = None,
                fallback_lineups,
                updated_stash // (in practice must be empty by construction)
              )

            case (Some(non_matching_lineup), _)
                if !lineup_matcher(shot.shot_min)(non_matching_lineup) =>
              // 2.2] This lineup starts after the shot, so no match but keep it in the stash
              RecursionState(
                curr_lineup = None,
                fallback_lineups,
                maybe_matching_lineup.toList ++ updated_stash
              )

            case (Some(matching_lineup), Nil)
                if shot.shot_min < matching_lineup.end_min =>
              // 2.3] Strictly inside the lineup and the previous lineup(s) didn't match (so no need for
              // more complex logic)
              RecursionState(
                maybe_matching_lineup,
                fallback_lineups,
                updated_stash
              )

            case (Some(matching_lineup), _) =>
              // 2.4] Either we're in "pick from multiple lineps" logic (fallback.nonEmpty)
              // or we need to enter it (shot_min == end_min)

              /** Extracts the event strings from the shot direction */
              def pbp_event_str(
                  in: List[LineupEvent.RawGameEvent]
              ) =
                in.flatMap(ev =>
                  (if (shot.is_off) ev.team else ev.opponent).toList
                )

              // look through the raw game events looking for a PbP string match
              if (
                curr_pbp.forall {
                  pbp => // (f no curr_pbp then just take this lineup)
                    pbp_event_str(matching_lineup.raw_game_events)
                      .contains(pbp.event_string)
                }
              ) {
                RecursionState(
                  maybe_matching_lineup,
                  fallback_lineups ++
                    maybe_matching_lineup.toList,
                  // (we return all processed lineups, in case shots with the same are out of order)
                  updated_stash
                )
              } else { // this lineup matches but didn't match the PbP event so keep looking
                find_lineup_recurse(
                  RecursionState(
                    curr_lineup =
                      None, // (set this to None to force it to take a new lineup)
                    fallback_lineups = fallback_lineups ++
                      maybe_matching_lineup.toList,
                    stashed_lineups =
                      updated_stash // (move from the stash to the fallbacks)
                  )
                )
                // (we save the *first* matching lineup in case we can't find a matching PbP event)
              }
          }
      }
      // Top-level logic
      val post_recursion_state =
        if (
          curr_lineups.headOption.exists(
            _.start_min > shot.shot_min
          )
        ) {
          // Special case: we've gone past the lineup, just do nothing and wait for the shot
          // to catch up:
          RecursionState(
            curr_lineup = None,
            fallback_lineups = Nil,
            stashed_lineups = curr_lineups
          )
        } else {
          find_lineup_recurse(
            RecursionState(
              curr_lineups.headOption,
              fallback_lineups = Nil,
              stashed_lineups = curr_lineups.drop(1)
            )
          )
        }
      (
        post_recursion_state.curr_lineup,
        post_recursion_state.fallback_lineups
      ) match {
        case (maybe_matching_lineup, Nil) =>
          // (no fallbacks, just return the lineup)
          (
            maybe_matching_lineup,
            // (list of lineups to try next time, includes the current one):
            maybe_matching_lineup.toList ++ post_recursion_state.stashed_lineups
          )
        case (maybe_matching_lineup, fallback_lineups) =>
          // there are fallbacks, which means the "matching_lineup" must be one of them
          // so no need to add explicitly to the stash
          (
            maybe_matching_lineup.orElse(fallback_lineups.headOption),
            // not using the fallback, keep them all to try next time
            // (also includes the current one):
            fallback_lineups ++ post_recursion_state.stashed_lineups
          )
      }
    }

    /* Pull out only the PbP events we need - shots and assists on either side */
    object ShotOrAssistFinder {
      def unapply(ev: Model.PlayByPlayEvent): Option[Model.MiscGameEvent] =
        ev match {
          case game_event: Model.MiscGameEvent =>
            game_event.event_string match {
              case EventUtils.ParseAssist(_)     => Some(game_event)
              case EventUtils.ParseShotMade(_)   => Some(game_event)
              case EventUtils.ParseShotMissed(_) => Some(game_event)
              case _                             => None
            }
          case _ => None
        }
    }

    /** Is it a 3, a 2, or an assist */
    def shot_value(event_str: String): Int = event_str match {
      case EventUtils.ParseAssist(_)             => 0
      case EventUtils.ParseThreePointerMade(_)   => 3
      case EventUtils.ParseThreePointerMissed(_) => 3
      case EventUtils.ParseTwoPointerMade(_)     => 2
      case EventUtils.ParseTwoPointerMissed(_)   => 2
      case _                                     => -1
    }

    /** Returns game events matching the description */
    private def pbp_clump_matcher(matcher: Model.MiscGameEvent => Boolean)(
        pbp_it: Iterator[Model.PlayByPlayEvent]
    ): Option[Model.MiscGameEvent] = pbp_it
      .find {
        case game_event: Model.MiscGameEvent =>
          ShotOrAssistFinder.unapply(game_event).exists(matcher)
        case _ => false
      }
      .map(_.asInstanceOf[Model.MiscGameEvent])

    /** Get all PBP entries with the same time */
    def find_pbp_clump(
        shot_time: Double,
        pbp_it: Iterator[Model.PlayByPlayEvent],
        curr_pbp_clump: List[Model.MiscGameEvent],
        maybe_next_pbp_event: Option[Model.MiscGameEvent]
    ): (List[Model.MiscGameEvent], Option[Model.MiscGameEvent]) = {

      @tailrec
      def find_pbp_clump_recurse(
          tmp_curr_pbp_clump: List[Model.MiscGameEvent],
          tmp_maybe_next_pbp_event: Option[Model.MiscGameEvent]
      ): (List[Model.MiscGameEvent], Option[Model.MiscGameEvent]) = {
        tmp_maybe_next_pbp_event match {
          case None if pbp_it.hasNext =>
            // get next pbp event
            find_pbp_clump_recurse(
              tmp_curr_pbp_clump,
              pbp_clump_matcher(_.min >= shot_time)(pbp_it)
              // (grab another event, recurse to figure out what to do with it)
            )
          case None =>
            // end of the PbP events
            (tmp_curr_pbp_clump, None)
          case Some(next_pbp_event) if next_pbp_event.min < shot_time =>
            // next pbp is before the clump, discard it and check the next one
            find_pbp_clump_recurse(
              tmp_curr_pbp_clump,
              pbp_clump_matcher(_.min >= shot_time)(pbp_it)
              // (grab another event, recurse to figure out what to do with it)
            )
          case Some(next_pbp_event) if next_pbp_event.min == shot_time =>
            // next pbp is part of clump
            find_pbp_clump_recurse(
              tmp_curr_pbp_clump ++ List(next_pbp_event),
              pbp_clump_matcher(_.min >= shot_time)(pbp_it)
              // (grab another event, recurse to figure out what to do with it)
            )
          case _ => // next pbp is not part of clump (_.min > shot_time) so we're done for now
            (tmp_curr_pbp_clump, tmp_maybe_next_pbp_event)
        }
      }

      val clump_time_matches = curr_pbp_clump.filter(_.min == shot_time)
      if (clump_time_matches.nonEmpty) { // Still some events from prev call left over
        (clump_time_matches, maybe_next_pbp_event)
      } else { // get the next clump, having flushed
        find_pbp_clump_recurse(
          tmp_curr_pbp_clump = Nil,
          maybe_next_pbp_event
        )
      }

    }

    /** Gets a player code/id, using enrichment if it's a shot from _team_ (not
      * opponent)
      */
    def extract_player_from_ev(
        shot: ShotEvent,
        pbp_event: Model.MiscGameEvent,
        tidy_ctx: LineupErrorAnalysisUtils.TidyPlayerContext
    ): Option[LineupEvent.PlayerCodeId] =
      EventUtils.ParseAnyPlay
        .unapply(pbp_event.event_string)
        .map { v1_player_name =>
          val player_name = name_in_v0_format(v1_player_name)
          if (shot.is_off) {
            val (tidier_player_name, _) =
              LineupErrorAnalysisUtils.tidy_player(player_name, tidy_ctx)
            ExtractorUtils.build_player_code(
              tidier_player_name,
              Some(tidy_ctx.box_lineup.team.team)
            )
          } else {
            ExtractorUtils.build_player_code(
              player_name,
              None
            )
          }
        }

    /** Does the player code/id in the PbP event match the one in the shot */
    def matching_player(
        shot: ShotEvent,
        pbp_event: Model.MiscGameEvent,
        tidy_ctx: LineupErrorAnalysisUtils.TidyPlayerContext,
        code_match: Boolean
    ): Boolean =
      extract_player_from_ev(shot, pbp_event, tidy_ctx).exists(player => {
        if (code_match) {
          shot.shooter.exists(
            _.code == player.code
          )
        } else {
          shot.shooter.contains(player)
        }
      })

    /** We probably can't exactly tell the shot type from the distance because
      * of the approx's in the data But we can rule out obvious 2s and 3s when
      * the PbP event doesn't match (and also make sure we only match misses
      * with misses and makes with makes)
      */
    def right_kind_of_shot(
        shot: ShotEvent,
        pbp_event: Model.MiscGameEvent
    ): Boolean = {
      val ev_shot_value = shot_value(pbp_event.event_string)
      val ev_shot_made =
        EventUtils.ParseShotMade.unapply(pbp_event.event_string).isDefined
      val shot_made = shot.pts > 0
      val definitely_2 =
        shot.dist < 21 // 21.6 to be more exact but we give a little leeway
      val definitely_3 =
        shot.dist > 22.5 // 22.1 to be more exact but we give a little leeway

      (shot_made == ev_shot_made) && (
        (definitely_2 && ev_shot_value == 2) ||
          (definitely_3 && ev_shot_value == 3) ||
          (!definitely_2 && !definitely_3)
      )
    }
  }

  ////////////////////////////

  /** v1 format removed the list of starters, so we have to infer it */
  def inject_starting_lineup_into_box(
      sorted_pbp_events: List[Model.PlayByPlayEvent],
      box_lineup: LineupEvent,
      external_roster: (List[String], List[RosterEntry]),
      format_version: Int
  ): LineupEvent = {
    case class StarterState(
        starters: Set[String],
        excluded: Set[String],
        last_sub_time: Double = 20.0
    ) {
      // Some state utils to handle the fact that the raw events aren't formatted but the box score/restore are
      lazy val formatted_starters: Set[String] =
        starters.map(ExtractorUtils.name_in_v0_format)
      lazy val formatted_non_starters: Set[String] =
        excluded.map(ExtractorUtils.name_in_v0_format)
    }
    val valid_players_set =
      external_roster._2.map(_.player_code_id.id.name).toSet

    val starter_info: StarterState =
      sorted_pbp_events.foldLeft(StarterState(Set(), Set())) {
        case (state, _)
            if state.starters.size >= 5 => // (we have all the starters)
          state

        case (state, ev: Model.SubEvent)
            if !valid_players_set.contains(
              ExtractorUtils.name_in_v0_format(ev.player_name)
            ) =>
          // Ignore mis-spellings in sub-events
          state

        case (state, Model.GameBreakEvent(min, _)) =>
          // Reset the last sub time
          state.copy(last_sub_time = min)

        case (state, Model.SubOutEvent(time, _, player))
            if !state.excluded.contains(player) =>
          // Subbed out, so if not excluded (ie we haven't seen them subbed-in) then is a starter
          state.copy(starters = state.starters + player, last_sub_time = time)

        case (state, Model.SubInEvent(time, _, player))
            if !state.starters.contains(player) =>
          // Subbed in, so if not a starter is definitely not a starter
          state.copy(excluded = state.excluded + player, last_sub_time = time)

        case (state, ev: Model.MiscGameEvent)
            if ev.is_team_dir && ev.min > state.last_sub_time =>
          ev.event_string match {
            case EventUtils.ParseAnyPlay(player)
                if valid_players_set
                  .contains(player) && !state.excluded.contains(
                  player
                ) && !state.starters.contains(
                  player
                ) =>
              // A player is mentioned, they have not yet been subbed-in, and isn't concurrent with a sub event
              // so they must be a starter
              state.copy(starters = state.starters + player)
            case _ =>
              state
          }
        case (state, _) => state
      }
    val (starters, probably_not_starters) =
      box_lineup.players.partition(p =>
        starter_info.formatted_starters
          .contains(p.id.name)
      )

    if (starters.size >= 5) {
      box_lineup.copy(players = starters ++ probably_not_starters)
    } else {
      // Pathological case where a starter played all 40 mins without ever getting mentioned (a "40 trillian"!)
      // Since we didn't pull out minutes from the box score, we'll just pick a rando who didn't appear
      // in the excluded set and call them the starter. I cannot understate how little I expect this to happen!
      val (definitely_not_starters, just_possibly_starters) =
        probably_not_starters
          .partition(p =>
            starter_info.formatted_non_starters
              .contains(p.id.name)
          )
      box_lineup.copy(players =
        starters ++ just_possibly_starters ++ definitely_not_starters
      )
    }
  }

}
object PlayByPlayUtils extends PlayByPlayUtils
