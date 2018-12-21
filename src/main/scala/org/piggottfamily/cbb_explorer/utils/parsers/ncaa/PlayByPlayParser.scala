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

//TODO:
/*
not sure why the sub here isn't working...
LineupId("AaWi_AnCo_ErAy_IvBe_RiLi"),
List(
  PlayerCodeId("AaWi", PlayerId("Wiggins, Aaron")),
  PlayerCodeId("AnCo", PlayerId("Cowan, Anthony")),
  PlayerCodeId("ErAy", PlayerId("Ayala, Eric")),
  PlayerCodeId("IvBe", PlayerId("Bender, Ivan")),
  PlayerCodeId("RiLi", PlayerId(",RICKY LINDO JR"))
*/

  import ExtractorUtils._
  import LineupUtils._

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `ncaa.parse_playbyplay` = "ncaa.parse_playbyplay"

  // Holds all the HTML parsing logic
  protected object builders {

    def team_finder(doc: Document): List[String] =
      (doc >?> elementList("div#contentarea table.mytable[width=50%] td a[href]"))
        .getOrElse(Nil).map(_.text)

    def event_finder(doc: Document): List[Element] =
      (doc >?> elementList("table.mytable tr:has(td.smtext)")).filter(_.nonEmpty).getOrElse(Nil)

    def event_time_finder(event: Element): Option[String] =
      (event >?> element("td.smtext:eq(0)")).map(_.text).filter(_.nonEmpty)

    def event_score_finder(event: Element): Option[String] =
      (event >?> element("td.smtext:eq(2)")).map(_.text).filter(_.nonEmpty)

    def game_event_finder(event: Element): Option[String] =
      (event >?> element("td.boldtext")).map(_.text).filter(_.nonEmpty)

    private def index(is: Boolean, want: Boolean): Int = if (is == want) 1 else 3

    def event_team_finder(event: Element, target_team_first: Boolean): Option[String] =
      (event >?> element(s"td.smtext:eq(${index(is = true, target_team_first)})"))
        .map(_.text).filter(_.nonEmpty)

    def event_opponent_finder(event: Element, target_team_first: Boolean): Option[String] =
      (event >?> element(s"td.smtext:eq(${index(is = false, target_team_first)})"))
        .map(_.text).filter(_.nonEmpty)
  }

  /** Combines the different methods to build a set of lineup events */
  def create_lineup_data(
    filename: String,
    in: String,
    box_lineup: LineupEvent
  ): Either[List[ParseError], (List[LineupEvent], List[LineupEvent])] = {
    parse_game_events(filename, in, box_lineup.team.team).map { reversed_events =>
      build_partial_lineup_list(reversed_events.toIterator, box_lineup)
    }.map {
      _.map(enrich_lineup).partition(validate_lineup)
    }
  }

  /** Creates a list of raw play-by-play events from the HTML
   *  fixes the dates, and injects game breaks
  *   The returned list is reversed
  */
  protected def parse_game_events(
    filename: String,
    in: String,
    target_team: TeamId
  ): Either[List[ParseError], List[Model.PlayByPlayEvent]] =
  {
    val doc_request_builder = ParseUtils.build_request[Document](`ncaa.parse_playbyplay`, filename) _
    val single_error_completer = ParseUtils.enrich_sub_error(`ncaa.parse_playbyplay`, filename) _
    val browser = JsoupBrowser()

    for {
      doc <- doc_request_builder(browser.parseString(in))

      team_info <- parse_team_name(
        builders.team_finder(doc), target_team
      ).left.map(single_error_completer)

      (_, _, target_team_first) = team_info //SI-5589

      html_events <- builders.event_finder(doc) match {
        case events if events.nonEmpty => Right(events)
        case _ =>
          Left(List(ParseUtils.build_sub_error(`ncaa.parse_playbyplay`)(
            s"No play by play events found [$doc]"
          )))
      }
      model_events <- html_events.map(parse_game_event(_, target_team_first)).sequence
    } yield enrich_and_reverse_game_events(model_events.flatten)
  }

  /** Some things that need to happen:
    * - insert game break events
    * - turn the descending times into ascending times
    * Assumes that game events are received in the correct order (earliest to latest)
    * and returns them in the opposite order (latest to earliest)
   */
  protected def enrich_and_reverse_game_events(
    in: List[Model.PlayByPlayEvent]
  ): List[Model.PlayByPlayEvent] =
  {
    def ascend_minutes(ev: Model.PlayByPlayEvent, period: Int): Model.PlayByPlayEvent = {
      val total_duration = duration_from_period(period)
      val new_min = total_duration - ev.min
      ev.with_min(new_min)
    }
    case class State(
      period: Int,
      last: Option[Model.PlayByPlayEvent],
      game_events: List[Model.PlayByPlayEvent]
    )

    val starting_state = State(1, None, Nil)
    val end_state = in.foldLeft(starting_state) {
      (state, event) => state.last match {
        case None =>
          State(state.period,
            Some(event),
            ascend_minutes(event, state.period) :: Nil
          )
        case Some(next_event) if event.min > next_event.min => // game break!
          val game_break = Model.GameBreakEvent(duration_from_period(state.period))
          val new_period = state.period + 1
          State(new_period,
            Some(event),
            ascend_minutes(event, new_period) :: game_break :: state.game_events
          )
        case _ =>
          State(state.period,
            Some(event),
            ascend_minutes(event, state.period) :: state.game_events
          )
      }
    }
    Model.GameEndEvent(duration_from_period(end_state.period)) :: end_state.game_events
  }

  /** Creates a Model.PlayByPlayEvent from the table entry + inserts game breaks
    * Game events are returned earliest to latest
  */
  protected def parse_game_event(
    el: Element,
    target_team_first: Boolean
  ): Either[List[ParseError], List[Model.PlayByPlayEvent]] =
  {
    val single_error_completer = ParseUtils.enrich_sub_error(`ncaa.parse_playbyplay`, `parent_fills_in`) _

    // Is it a game event
    builders.game_event_finder(el).map { _ =>
      Right(Nil) //TODO: for now just ignore these, later on can use timeouts to split lineups maybe?
    }.getOrElse {
      for {
        _ <- Right(()) //(just determines the for type)

        score_or_error = parse_game_score(el).left.map(single_error_completer)
        time_or_error = parse_desc_game_time(el).left.map(single_error_completer)

        score_and_time <- (score_or_error, time_or_error).parMapN((_, _))
        ((score_str, raw_score), (time_str, time_mins)) = score_and_time

        // Ensure team score is first
        score = if (target_team_first) raw_score else raw_score.copy(
          scored = raw_score.allowed,
          allowed = raw_score.scored
        )

        event <- (
          builders.event_team_finder(el, target_team_first),
          builders.event_opponent_finder(el, target_team_first)
        ) match {
          case ParseTeamSubIn(player) =>
            Right(Model.SubInEvent(time_mins, player))
          case ParseTeamSubOut(player) =>
            Right(Model.SubOutEvent(time_mins, player))

          case (Some(team), None) =>
            Right(Model.OtherTeamEvent(time_mins, score, s"$time_str,$score_str,$team"))
          case (None, Some(oppo)) =>
            Right(Model.OtherOpponentEvent(time_mins, score, s"$time_str,$score_str,$oppo"))

          case (Some(team), Some(oppo)) =>
            Left(List(ParseUtils.build_sub_error(`ncaa.parse_playbyplay`)(
              s"Not allowed both team and opponent events in the same entry [$el]: [$team] vs [$oppo]"
            )))
          case unsupported_entry => //(line break for some reason)
            Left(List(ParseUtils.build_sub_error(`ncaa.parse_playbyplay`)(
              s"Must have either team or opponent event in one entry [$el]: [$unsupported_entry]"
            )))
        }
      } yield List(event)
    }
  }

  /** Parse a descending time of the form NN:MM into an ascending time */
  protected def parse_game_score(el: Element)
    : Either[ParseError, (String, Game.Score)] =
  {
    val score_regex = "([0-9]+)[-]([0-9]+)".r
    val `game_score` = "game_score"
    builders.event_score_finder(el) match {
      case None =>
        Left(ParseUtils.build_sub_error(`game_score`)(
          s"Could not find score in [$el]"
        ))
      case Some(str @ score_regex(team, oppo)) =>
        Right((str, Game.Score(team.toInt, oppo.toInt)))
      case Some(str) =>
        Left(ParseUtils.build_sub_error(`game_score`)(
          s"Could not find parse score [A-B] from [$str] in [$el]"
        ))
    }
  }

  /** Parse a descending time of the form NN:MM into time (still descending, will
   *  make it ascend in a separate stateful block of code)
   */
  protected def parse_desc_game_time(el: Element)
    : Either[ParseError, (String, Double)] =
  {
    val `game_time` = "game_time"
    val time_regex = "([0-9]+):([0-9]+)(?:[:]([0-9]+))?".r
    builders.event_time_finder(el) match {
      case None =>
        Left(ParseUtils.build_sub_error(`game_time`)(
          s"Could not find time in [$el]"
        ))
      case Some(str @ time_regex(min, secs, maybe_csecs)) =>
        val descending_mins =
          min.toInt*1.0 + secs.toInt/60.0
          + Option(maybe_csecs).map(_.toInt).getOrElse(0)/6000.0
        Right((str, descending_mins))
      case Some(str) =>
        Left(ParseUtils.build_sub_error(`game_time`)(
          s"Could not find parse time [MM:SS] from [$str] in [$el]"
        ))
    }
  }

  // Very low-level parser

  protected object ParseTeamSubIn {
    private val sub_regex_in = "(.+) +Enters Game".r
    private val sub_regex_in_new_format = "(.+), +substitution in".r
    def unapply(x: (Option[String], Option[String])): Option[String] = x match {
      case (Some(sub_regex_in(player)), None) => Some(player)
      case (Some(sub_regex_in_new_format(player)), None) => Some(player)
      case _ => None
    }
  }
  protected object ParseTeamSubOut {
    private val sub_regex_out = "(.+) +Leaves Game".r
    private val sub_regex_in_out_format = "(.+), +substitution out".r
    def unapply(x: (Option[String], Option[String])): Option[String] = x match {
      case (Some(sub_regex_out(player)), None) => Some(player)
      case (Some(sub_regex_in_out_format(player)), None) => Some(player)
      case _ => None
    }
  }

}
object PlayByPlayParser extends PlayByPlayParser
