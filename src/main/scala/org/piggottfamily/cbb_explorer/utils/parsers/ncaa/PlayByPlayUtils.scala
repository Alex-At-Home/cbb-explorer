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
        val curr_lineup = find_lineup(
          ev => ev.end_min >= shot.shot_min && shot.shot_min >= ev.start_min,
          state.lineup_it,
          state.curr_lineup
        )
        val (pbp_clump, maybe_next_pbp_event) =
          find_pbp_clump(
            shot.shot_min,
            state.pbp_it,
            state.curr_pbp_clump,
            state.maybe_next_pbp_event
          )

        val (maybe_enriched_shot, remaining_pbp_events) =
          (
            curr_lineup,
            pbp_clump
              .collect { // (ignore game events from the other team than took the shot)
                case ev: Model.OtherTeamEvent if shot.is_off      => ev
                case ev: Model.OtherOpponentEvent if !shot.is_off => ev
              }
          ) match {
            case (Some(lineup), pbp_clump) if pbp_clump.nonEmpty =>
              val (pbp_shots, pbp_assists) =
                pbp_clump.partition(ev => shot_value(ev.event_string) > 0)

              val maybe_selected_pbp =
                pbp_shots.filter(ev => right_kind_of_shot(shot, ev)) match {
                  case Nil => None
                  case candidate_matches =>
                    val player_filtered_candidate_matches =
                      candidate_matches.filter(ev =>
                        matching_player(shot, ev, tidy_ctx)
                      )

                    player_filtered_candidate_matches match {
                      case Nil if candidate_matches.size == 1 =>
                        // only one candidate match, so just use it
                        // Warning since this PbP event must match this shot, but the player codes don't match up
                        println(
                          s"[enrich_shot_events_with_pbp] WARN: player mismatch for shot [$shot] vs [$candidate_matches]"
                        )
                        candidate_matches.headOption
                      case Nil =>
                        // Too many "wrong" candidate matches, bailing
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
                  // Look for assists:
                  val maybe_assist_pbp =
                    pbp_assists
                      .filter(_ => shot.pts > 0) // (can't assist a missed shot)
                      .find(ev =>
                        !matching_player(shot, ev, tidy_ctx)
                      ) // (can't self-assist!)

                  val shot_val = shot_value(selected_pbp.event_string)
                  (
                    Some(
                      shot.copy(
                        shooter = shot.shooter.filter(_ =>
                          shot.is_off
                        ), // (discard oppo shooters)
                        lineup_id = lineup.lineup_id,
                        players = lineup.players,
                        pts = shot.pts * shot_val,
                        value = shot_val,
                        is_assisted =
                          Some(maybe_assist_pbp.isDefined).filter(_ == true),
                        assisted_by = maybe_assist_pbp.flatMap(ev =>
                          extract_player_from_ev(shot, ev, tidy_ctx)
                        ),
                        in_transition = Some(
                          selected_pbp.event_string.contains("fastbreak")
                        ).filter(_ == true)
                      )
                    ),
                    pbp_clump.filterNot(ev => // (clump minus matching events)
                      ev == selected_pbp || maybe_assist_pbp.contains(ev)
                    )
                  )
                case None =>
                  // (already added a warning for this case above)
                  (None, pbp_clump)
              }
            case _ => // No lineup, this is basically an internal logic error
              println(
                s"[enrich_shot_events_with_pbp] WARN: discarding unmatched shot [$shot], NO_LINEUP"
              )
              (None, pbp_clump)
          }
        state.copy(
          enriched_shot_events =
            state.enriched_shot_events ++ maybe_enriched_shot.toList,
          curr_pbp_clump = remaining_pbp_events,
          maybe_next_pbp_event = maybe_next_pbp_event,
          curr_lineup = curr_lineup
        )
    }
    finishingState.enriched_shot_events
  }

  ///////////////////// Utils for enrich_shot_events_with_pbp

  /** Separated out fiddly details for testing purposes */
  protected object ShotEnrichmentUtils {

    /** State object for matching up PbP events with shots */
    case class EnrichmentState(
        enriched_shot_events: List[ShotEvent] = Nil,
        pbp_it: Iterator[Model.PlayByPlayEvent],
        curr_pbp_clump: List[Model.MiscGameEvent] = Nil,
        maybe_next_pbp_event: Option[Model.MiscGameEvent] = None,
        lineup_it: Iterator[LineupEvent],
        curr_lineup: Option[LineupEvent] = None
    )

    /** Finds the lineup event that encompasses the shot */
    def find_lineup(
        matcher: LineupEvent => Boolean,
        lineup_it: Iterator[LineupEvent],
        curr_lineup: Option[LineupEvent]
    ): Option[LineupEvent] = curr_lineup match {
      case Some(ev) if matcher(ev) =>
        curr_lineup
      case _ =>
        lineup_it.find(matcher) // (advances iterator to next element)
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
            }
          case _ => None
        }
    }

    /** Is it a 3, a 2, or an assist */
    def shot_value(event_str: String): Int = event_str match {
      case EventUtils.ParseAssist(_)             => 0
      case EventUtils.ParseThreePointerMade(_)   => 3
      case EventUtils.ParseThreePointerMissed(_) => 3
      case _                                     => 2
    }

    /** Get all PBP entries with the same time */
    @tailrec
    def find_pbp_clump(
        shot_time: Double,
        pbp_it: Iterator[Model.PlayByPlayEvent],
        curr_pbp_clump: List[Model.MiscGameEvent],
        maybe_next_pbp_event: Option[Model.MiscGameEvent]
    ): (List[Model.MiscGameEvent], Option[Model.MiscGameEvent]) =
      maybe_next_pbp_event match {
        case None if pbp_it.hasNext =>
          // get next pbp event
          find_pbp_clump(
            shot_time,
            pbp_it,
            curr_pbp_clump,
            pbp_it
              .collect { case game_event: Model.MiscGameEvent =>
                game_event
              }
              .find {
                case ShotOrAssistFinder(ev) => ev.min == shot_time
                case _                      => false
              } // (keeps going until it finds the 1st event with a matching time, moves the it)
          )
        case None =>
          // end of the PbP events
          (curr_pbp_clump, None)
        case Some(next_pbp_event) if next_pbp_event.min < shot_time =>
          // next pbp is before the clump, discard it and check the next one
          find_pbp_clump(
            shot_time,
            pbp_it,
            curr_pbp_clump,
            pbp_it
              .collect { case game_event: Model.MiscGameEvent =>
                game_event
              }
              .find {
                case ShotOrAssistFinder(ev) => ev.min == shot_time
                case _                      => false
              } // (keeps going until it finds the 1st event with a matching time, moves the it)
          )
        case Some(next_pbp_event) if next_pbp_event.min == shot_time =>
          // next pbp is part of clump
          find_pbp_clump(
            shot_time,
            pbp_it,
            curr_pbp_clump ++ List(next_pbp_event),
            pbp_it
              .collect { case game_event: Model.MiscGameEvent =>
                game_event
              }
              .find {
                case ShotOrAssistFinder(ev) => ev.min > shot_time
                case _                      => false
              }
          )
        case _ => // next pbp is not part of clump (_.min > shot_time) so we're done for now
          (curr_pbp_clump, maybe_next_pbp_event)
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
        .map { player_name =>
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
        tidy_ctx: LineupErrorAnalysisUtils.TidyPlayerContext
    ): Boolean = extract_player_from_ev(shot, pbp_event, tidy_ctx).exists(
      shot.shooter.contains
    )

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
}
object PlayByPlayUtils extends PlayByPlayUtils
