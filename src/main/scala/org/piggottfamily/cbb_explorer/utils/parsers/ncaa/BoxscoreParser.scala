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
trait BoxscoreParser {

  import ExtractorUtils._

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `ncaa.parse_boxscore` = "ncaa.parse_boxscore"

  // Holds all the HTML parsing logic
  protected object builders {

    def team_finder(doc: Document): Option[Element] =
      (doc >?> element("div#contentarea br + table.mytable tr:eq(1) td a[href]"))

    def opponent_finder(doc: Document): Option[Element] =
      (doc >?> element("div#contentarea br + table.mytable tr:eq(2) td a[href]"))

    def date_finder(doc: Document): Option[Element] =
      (doc >?> element("td.boldtext:contains(Game Date:) + td"))

    def boxscore_finder(doc: Document): Option[List[Element]] =
      (doc >?> elementList("div.header_menu + table.mytable td a[href]")).filter(_.nonEmpty)
  }

  /** Gets the boxscore lineup from the HTML page */
  def get_box_lineup(in: String, filename: String, year: Year)
    : Either[List[ParseError], LineupEvent] =
  {
    val browser = JsoupBrowser()

    // Error reporters
    val doc_request_builder = ParseUtils.build_request[Document](`ncaa.parse_boxscore`, filename) _
    val single_error_completer = ParseUtils.enrich_sub_error(`ncaa.parse_boxscore`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      period <- parse_period_from_filename(filename)
                  .left.map(single_error_completer)

      team <- parse_team_name(
        builders.team_finder(doc), "team"
      ).left.map(single_error_completer)

      opponent <- parse_team_name(
        builders.opponent_finder(doc), "opponent"
      ).left.map(single_error_completer)

      date <- parse_date(
        builders.date_finder(doc)
      ).left.map(single_error_completer)

      starting_lineup <- parse_players_from_boxscore(
        builders.boxscore_finder(doc)
      ).left.map(single_error_completer)

    } yield LineupEvent(
      date,
      start_min = start_time_from_period(period),
      end_min = start_time_from_period(period),
      duration_mins = 0.0,
      score_diff = 0,
      team = TeamSeasonId(TeamId(team), year),
      opponent = TeamSeasonId(TeamId(opponent), year),
      lineup_id = LineupEvent.LineupId.unknown,
      players = starting_lineup.map(build_player_code),
      players_in = Nil,
      players_out = Nil,
      raw_team_events = Nil,
      raw_opponent_events = Nil,
      team_stats = LineupEventStats.empty,
      opponent_stats = LineupEventStats.empty
    )
  }

  // Utils

  /** Gets the box score's period from the filename */
  protected def parse_period_from_filename(filename: String)
    : Either[ParseError, Int] =
  {
    val filename_parser = "[^_]+_p([0-9]+)[.][^.]*".r // eg test_p<period>.html

    filename match {
      case filename_parser(period_str) =>
        Right(period_str.toInt)
      case _ =>
        Left(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Could not parse period out of filename [$filename] via [$filename_parser]"
        ))
    }
  }

  /** Pulls team name from table element, just encapsulates error handling */
  protected def parse_team_name(name: Option[Element], type_str: String)
    : Either[ParseError, String] =
  {
    name.map(_.text).map(Right(_)).getOrElse {
      Left(ParseUtils.build_sub_error(`parent_fills_in`)(
        s"Could not find [$type_str] name"
      ))
    }
  }

  /** Parses dates of the format '12/03/2017' */
  protected def parse_date(date: Option[Element]):
    Either[ParseError, DateTime] =
  {
    val formatter = DateTimeFormat.forPattern("MM/dd/yyyy")

    date.map(_.text).map(_.trim).map { date_str =>
      Try(
        Right(formatter.parseDateTime(date_str))
      ).toOption.getOrElse {
        Left(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Unexpected date format: [$date_str]"
        ))
      }
    }.getOrElse {
      Left(ParseUtils.build_sub_error(`parent_fills_in`)(
        s"Could not find date"
      ))
    }
  }

  /** Gets the list of starters from the boxscore */
  protected def parse_players_from_boxscore(boxscore_table: Option[List[Element]])
    : Either[ParseError, List[String]] =
  {
    boxscore_table.map { rows =>
      if (rows.size >= 5) {
        Right(rows.map(_.text))
      } else {
        Left(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Not enough rows in boxscore table: [$rows]"
        ))
      }
    }.getOrElse {
      Left(ParseUtils.build_sub_error(`parent_fills_in`)(
        s"Could not find boxscore table"
      ))
    }
  }

}
