package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._
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

/** Parses the game HTML (or game subsets of the team HTML) */
trait PlayByPlayParser {

  import ExtractorUtils._
  import LineupUtils._

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `ncaa.parse_playbyplay` = "ncaa.parse_playbyplay"

  // Holds all the HTML parsing logic
  protected trait base_builders {
    def team_finder(doc: Document): List[String]
    def event_finder(doc: Document): List[Element]
    def event_time_finder(event: Element): Option[String]
    def event_score_finder(event: Element): Option[String]
    def game_event_finder(event: Element): Option[String]
    def event_team_finder(
        event: Element,
        target_team_first: Boolean
    ): Option[String]
    def event_opponent_finder(
        event: Element,
        target_team_first: Boolean
    ): Option[String]
  }
  protected object v0_builders extends base_builders {

    // TODO: this doesn't work on 2019- any more, need to remove the 50% line
    def team_finder(doc: Document): List[String] = // 2020+ is 40%, 2019- is 50%
      (doc >?> elementList(
        "div#contentarea table.mytable[width~=[45]0%] td a[href]"
      ))
        .getOrElse(Nil)
        .map(_.text)

    def event_finder(doc: Document): List[Element] =
      (doc >?> elementList("table.mytable tr:has(td.smtext)"))
        .filter(_.nonEmpty)
        .getOrElse(Nil)

    def event_time_finder(event: Element): Option[String] =
      (event >?> element("td.smtext:eq(0)")).map(_.text).filter(_.nonEmpty)

    def event_score_finder(event: Element): Option[String] =
      (event >?> element("td.smtext:eq(2)")).map(_.text).filter(_.nonEmpty)

    def game_event_finder(event: Element): Option[String] =
      (event >?> element("td.boldtext:not(.smtext)"))
        .map(_.text)
        .filter(_.nonEmpty)

    private def index(is: Boolean, want: Boolean): Int =
      if (is == want) 1 else 3

    def event_team_finder(
        event: Element,
        target_team_first: Boolean
    ): Option[String] =
      (event >?> element(
        s"td.smtext:eq(${index(is = true, target_team_first)})"
      ))
        .map(_.text)
        .filter(_.nonEmpty)

    def event_opponent_finder(
        event: Element,
        target_team_first: Boolean
    ): Option[String] =
      (event >?> element(
        s"td.smtext:eq(${index(is = false, target_team_first)})"
      ))
        .map(_.text)
        .filter(_.nonEmpty)
  }
  protected object v1_builders extends base_builders {

    def team_finder(doc: Document): List[String] =
      (doc >?> elementList("table[align=center] > tbody a > img[alt]"))
        .getOrElse(Nil)
        .map(_.attr("alt"))

    def event_finder(doc: Document): List[Element] =
      (doc >?> elementList(
        // (find anything with a valid time string in the table)
        "div.card-body > table.table > tbody tr:matches([0-9]+:[0-9]+:[0-9]+)"
      ))
        .filter(_.nonEmpty)
        .getOrElse(Nil)

    def event_time_finder(event: Element): Option[String] =
      (event >?> element("td:eq(0)")).map(_.text).filter(_.nonEmpty)

    def event_score_finder(event: Element): Option[String] =
      (event >?> element("td:eq(2)")).map(_.text).filter(_.nonEmpty)

    def game_event_finder(event: Element): Option[String] =
      (event >?> element("td.boldtext"))
        .map(_.text)
        .filter(_.nonEmpty)

    private def index(is: Boolean, want: Boolean): Int =
      if (is == want) 1 else 3

    def event_team_finder(
        event: Element,
        target_team_first: Boolean
    ): Option[String] =
      (event >?> element(
        s"td:eq(${index(is = true, target_team_first)})"
      ))
        .map(_.text)
        .filter(_.nonEmpty)

    def event_opponent_finder(
        event: Element,
        target_team_first: Boolean
    ): Option[String] =
      (event >?> element(
        s"td:eq(${index(is = false, target_team_first)})"
      ))
        .map(_.text)
        .filter(_.nonEmpty)
  }
  protected var builders_from_version = Array(v0_builders, v1_builders)

  /** v1 format removed the list of starters, so we have to infer it */
  def inject_starting_lineup_into_box(
      filename: String,
      in: String,
      box_lineup: LineupEvent,
      external_roster: (List[String], List[RosterEntry]),
      format_version: Int
  ): Either[List[ParseError], LineupEvent] = {
    val builders = builders_from_version(format_version)
    parse_game_events(
      filename,
      in,
      box_lineup.team.team,
      box_lineup.team.year,
      builders,
      enrich = false
    ).map { game_events =>
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
        game_events.foldLeft(StarterState(Set(), Set())) {
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
              if ev.is_team_dir && ev.min < state.last_sub_time =>
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

  /** Combines the different methods to build a set of lineup events */
  def create_lineup_data(
      filename: String,
      in: String,
      box_lineup: LineupEvent,
      format_version: Int
  ): Either[List[ParseError], (List[LineupEvent], List[LineupEvent])] = {
    val player_codes = box_lineup.players.map(_.code).toSet
    val builders = builders_from_version(format_version)

    parse_game_events(
      filename,
      in,
      box_lineup.team.team,
      box_lineup.team.year,
      builders
    ).map { reversed_events =>
      // There is a weird bug that has happened one time where the scores got swapped
      // So we'll identify and fix this case
      fix_possible_score_swap_bug(
        build_partial_lineup_list(reversed_events.toIterator, box_lineup),
        box_lineup
      )
    }.map { events =>
      val processed_events = events.map(enrich_lineup _)

      // Calculate possessions per lineup
      var lineups_with_poss = PossessionUtils.calculate_possessions(
        processed_events
      )

      // Get good and bad lineups (together with context)
      // Use the context to fix the bad lineups if possible

      val tmp_lineups = lineups_with_poss.map(Some(_))
      val zip_lineups = tmp_lineups zip (tmp_lineups.drop(1) ++ List(None))

      val (good_lineups, bad_lineups) = zip_lineups.partition {
        case (Some(e), e_next) =>
          LineupErrorAnalysisUtils
            .validate_lineup(e, box_lineup, player_codes)
            .isEmpty
      }
      val bad_lineup_clumps = LineupErrorAnalysisUtils.clump_bad_lineups(
        bad_lineups.flatMap { case (opt_e, maybe_e) =>
          opt_e.map((_, maybe_e)).toList
        }
      )
      val fixed_or_not = bad_lineup_clumps.map(clump =>
        LineupErrorAnalysisUtils
          .analyze_and_fix_clumps(clump, box_lineup, player_codes)
      )
      val final_good_lineups = good_lineups.flatMap(_._1.toList) ++
        fixed_or_not.flatMap(_._1)
      val final_bad_lineups = fixed_or_not.flatMap(_._2.evs)

      (
        final_good_lineups,
        final_bad_lineups.map(ev =>
          ev.copy( // at the last moment, add the player_count_error
            player_count_error = Some(ev.players.size)
          )
        )
      )
    }
  }

  /** Creates a list of raw play-by-play events from the HTML fixes the dates,
    * and injects game breaks The returned list is reversed
    */
  protected def parse_game_events(
      filename: String,
      in: String,
      target_team: TeamId,
      year: Year,
      builders: base_builders,
      enrich: Boolean = true
  ): Either[List[ParseError], List[Model.PlayByPlayEvent]] = {
    val doc_request_builder =
      ParseUtils.build_request[Document](`ncaa.parse_playbyplay`, filename) _
    val single_error_completer =
      ParseUtils.enrich_sub_error(`ncaa.parse_playbyplay`, filename) _
    val browser = JsoupBrowser()

    for {
      doc <- doc_request_builder(browser.parseString(in))

      team_info <- parse_team_name(
        builders.team_finder(doc),
        target_team,
        year
      ).left.map(single_error_completer)

      (_, _, target_team_first) = team_info // SI-5589

      html_events <- builders.event_finder(doc) match {
        case events if events.nonEmpty => Right(events)
        case _ =>
          Left(
            List(
              ParseUtils.build_sub_error(`ncaa.parse_playbyplay`)(
                s"No play by play events found [$doc]"
              )
            )
          )
      }
      model_events <- html_events
        .map(parse_game_event(_, target_team_first, builders))
        .sequence
    } yield
      if (enrich) {
        enrich_and_reverse_game_events(model_events.flatten)
      } else {
        model_events.flatten
      }
  }

  /** Some things that need to happen:
    *   - insert game break events
    *   - turn the descending times into ascending times Assumes that game
    *     events are received in the correct order (earliest to latest) and
    *     returns them in the opposite order (latest to earliest)
    */
  protected def enrich_and_reverse_game_events(
      in: List[Model.PlayByPlayEvent]
  ): List[Model.PlayByPlayEvent] = {
    // (wow this is crazy, if the first event is 10mins into the game, it must be a women's game
    // which have quarters!)
    val is_women_game = in.headOption.exists(_.min <= 10);

    def ascend_minutes(
        ev: Model.PlayByPlayEvent,
        period: Int
    ): Model.PlayByPlayEvent = {
      val total_duration = duration_from_period(period, is_women_game)
      val new_min = total_duration - ev.min
      ev.with_min(new_min)
    }
    case class State(
        period: Int,
        last: Option[Model.PlayByPlayEvent],
        game_events: List[Model.PlayByPlayEvent]
    )

    val starting_state = State(1, None, Nil)
    val end_state = in.foldLeft(starting_state) { (state, event) =>
      state.last match {
        case None =>
          State(
            state.period,
            Some(event),
            ascend_minutes(event, state.period) :: Nil
          )
        case Some(prev_event) if event.min > prev_event.min + 1.1 =>
          // game break! (+1.1 for safety - sometimes events are mildly out of order, eg end of 2019/20 Florida-Missouri 1st half)
          val game_break = Model.GameBreakEvent(
            duration_from_period(state.period, is_women_game),
            event.score
          )
          val new_period = state.period + 1
          State(
            new_period,
            Some(event),
            ascend_minutes(event, new_period) :: game_break :: state.game_events
          )

        case Some(prev_event) =>
          val half_or_quarter_thresh =
            duration_from_period(
              state.period,
              is_women_game
            ) - duration_from_period(state.period - 1, is_women_game) - 0.5

          if ((event.min == 0) && (prev_event.min >= half_or_quarter_thresh)) {
            // (this is a rare corruption, the initial block of 2nd half results has 0s in the middle of it)
            // examples: 2020/21 N.C A&T (vs Alabama St.) / Jackson St. (vs South Carolina St.)
            val adjusted_event = event.with_min(prev_event.min)
            State(
              state.period,
              Some(adjusted_event),
              ascend_minutes(adjusted_event, state.period) :: state.game_events
            )

          } else {
            State(
              state.period,
              Some(event),
              ascend_minutes(event, state.period) :: state.game_events
            )
          }
      }
    }
    Model.GameEndEvent(
      duration_from_period(end_state.period, is_women_game),
      end_state.last.map(_.score).getOrElse(Game.Score(0, 0))
    ) :: end_state.game_events
  }

  /** Creates a Model.PlayByPlayEvent from the table entry + inserts game breaks
    * Game events are returned earliest to latest
    */
  protected def parse_game_event(
      el: Element,
      target_team_first: Boolean,
      builders: base_builders
  ): Either[List[ParseError], List[Model.PlayByPlayEvent]] = {
    val single_error_completer =
      ParseUtils.enrich_sub_error(`ncaa.parse_playbyplay`, `parent_fills_in`) _

    // Is it a game event
    builders
      .game_event_finder(el)
      .map { _ =>
        Right(
          Nil
        ) // TODO: for now just ignore these, later on can use timeouts to split lineups maybe?
      }
      .getOrElse {
        for {
          _ <- Right(()) // (just determines the for type)

          score_or_error = parse_game_score(el, builders).left.map(
            single_error_completer
          )
          time_or_error = parse_desc_game_time(el, builders).left.map(
            single_error_completer
          )

          score_and_time <- (score_or_error, time_or_error).parMapN((_, _))
          ((score_str, raw_score), (time_str, time_mins)) = score_and_time

          // Ensure team score is first
          score =
            if (target_team_first) raw_score
            else
              raw_score.copy(
                scored = raw_score.allowed,
                allowed = raw_score.scored
              )

          event <- (
            builders.event_team_finder(el, target_team_first),
            builders.event_opponent_finder(el, target_team_first)
          ) match {
            case EventUtils.ParseTeamSubIn(player) =>
              Right(Model.SubInEvent(time_mins, score, player))
            case EventUtils.ParseTeamSubOut(player) =>
              Right(Model.SubOutEvent(time_mins, score, player))

            case (Some(team), None) =>
              Right(
                Model.OtherTeamEvent(
                  time_mins,
                  score,
                  s"$time_str,$score_str,$team"
                )
              )
            case (None, Some(oppo)) =>
              Right(
                Model.OtherOpponentEvent(
                  time_mins,
                  score,
                  s"$time_str,$score_str,$oppo"
                )
              )

            case (Some(team), Some(oppo)) =>
              Left(
                List(
                  ParseUtils.build_sub_error(`ncaa.parse_playbyplay`)(
                    s"Not allowed both team and opponent events in the same entry [$el]: [$team] vs [$oppo]"
                  )
                )
              )
            case unsupported_entry => // (line break for some reason)
              Left(
                List(
                  ParseUtils.build_sub_error(`ncaa.parse_playbyplay`)(
                    s"Must have either team or opponent event in one entry [$el]: [$unsupported_entry]"
                  )
                )
              )
          }
        } yield List(event)
      }
  }

  /** Parse a descending time of the form NN:MM into an ascending time */
  protected def parse_game_score(
      el: Element,
      builders: base_builders
  ): Either[ParseError, (String, Game.Score)] = {
    val score_regex = "([0-9]+)[-]([0-9]+)".r
    val `game_score` = "game_score"
    builders.event_score_finder(el) match {
      case None =>
        Left(
          ParseUtils.build_sub_error(`game_score`)(
            s"Could not find score in [$el]"
          )
        )
      case Some(str @ score_regex(team, oppo)) =>
        Right((str, Game.Score(team.toInt, oppo.toInt)))
      case Some(str) =>
        Left(
          ParseUtils.build_sub_error(`game_score`)(
            s"Could not find parse score [A-B] from [$str] in [$el]"
          )
        )
    }
  }

  /** Parse a descending time of the form NN:MM into time (still descending,
    * will make it ascend in a separate stateful block of code)
    */
  protected def parse_desc_game_time(
      el: Element,
      builders: base_builders
  ): Either[ParseError, (String, Double)] = {
    val `game_time` = "game_time"
    builders.event_time_finder(el) match {
      case None =>
        Left(
          ParseUtils.build_sub_error(`game_time`)(
            s"Could not find time in [$el]"
          )
        )
      case Some(str @ EventUtils.ParseGameTime(descending_mins)) =>
        Right((str, descending_mins))
      case Some(str) =>
        Left(
          ParseUtils.build_sub_error(`game_time`)(
            s"Could not find parse time [MM:SS] from [$str] in [$el]"
          )
        )
    }
  }

}
object PlayByPlayParser extends PlayByPlayParser
