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
import scala.util.{Try, Success, Failure}

import java.net.URLEncoder

/** Parses a list of teams/conferences and extract (team, id, conference) triples */
trait TeamIdParser {

  import ExtractorUtils._

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `ncaa.get_team_ids` = "ncaa.get_team_ids"

  // Holds all the HTML parsing logic
  protected object builders {

    def team_row_finder(doc: Document): List[Element] =
      (doc >?> elementList("tr:has(td:has(a.skipMask))")).getOrElse(Nil)

    val id_regex_old = "/teams/([0-9]+)".r
    val id_regex_new = "/team/([0-9.]+)/[0-9]+".r

    def team_id_finder(e: Element): Option[String] =
      (e >?> element("a.skipMask")).flatMap(e => Option(e.attr("href"))).collect {
        case id_regex_new(id) => id
        case id_regex_old(id) => id
      }

    val team_conf_regex = "^(.*) \\(([A-Za-z0-9 .'-]+)\\)$".r

    def team_name_finder(e: Element, old_format: Boolean): Option[Either[String, (String, String)]] =
      (e >?> element("a.skipMask")).map(_.text).collect {
        case s if old_format => Left(s)
        case team_conf_regex(team, conf) => Right((team, conf))
      }

    val conference_regex = "([A-Za-z].*)".r

    def team_conference_finder(e: Element): Option[String] =
      (e >?> element("td:has(a.skipMask) + td")).map(_.text).collect {
        case conference_regex(conf) => conf
      }
  }

  /** Extract a list of triples for team, NCAA team id, and conference */
  def get_team_triples(filename: String, in: String, old_format: Boolean = false):
    Either[List[ParseError], List[(TeamId, String, ConferenceId)]] =
  {
    val doc_request_builder = ParseUtils.build_request[Document](`ncaa.get_team_ids`, filename) _
    val browser = JsoupBrowser()

    for {
      doc <- doc_request_builder(browser.parseString(in))
    } yield builders.team_row_finder(doc).flatMap { row =>
      (builders.team_name_finder(row, old_format),
        builders.team_id_finder(row),
        builders.team_conference_finder(row)
      ) match {
        case (Some(Right((name, conf))), Some(id), _) => // new format, the conf is in the name
          List((TeamId(name), id, ConferenceId(conf)))

        case (Some(Left(name)), Some(id), Some(conf)) => //old format, separate name
          List((TeamId(name), id, ConferenceId(conf)))
        case _ => //(didn't find one of the required fields, skip the row)
          Nil
      }
    }
  }

  /** Builds the array in the right format for the lineups-cli.sh files */
  def build_lineup_cli_array(in: List[(TeamId, String, ConferenceId)])
    : Map[ConferenceId, String] =
  {
    in.groupBy(_._3).mapValues(_.map { case (TeamId(team), team_id, _) =>
      s"   '$team_id::${URLEncoder.encode(team)}'"
    }.mkString("\n"))
  }

  /** Builds a team list for cbb-on-off-analyzer
      (insert the app-specific index key for each conference to get the JSON string)
  */
  def build_available_team_list(
    in_by_year: Map[String, List[(TeamId, String, ConferenceId)]]
  ) : Map[ConferenceId, String => String] =
  {
    in_by_year.toList.flatMap { case (key_year, team_list) =>
      team_list.map(t3 => (t3._3, t3._1, key_year)) // conf -> team -> year
    }.groupBy(_._1).mapValues { t3s_to_group_again => // (list of tuples, grouped by conf)
      def by_conf_str(key: String): String = {
        t3s_to_group_again.groupBy(_._2).map { case (TeamId(team), t3s) => //(list of tuples, grouped by team)
          val by_year_str_fn = t3s.map { case (_, _, year_str) =>
            s"""   { team: "$team", year: "$year_str", gender: "Men", index_template: "$key" },"""
          }.mkString("\n")
          s""" "$team": [
$by_year_str_fn
 ],"""
        }.mkString("\n")
      }
      by_conf_str _
    }
  }
}
object TeamIdParser extends TeamIdParser
