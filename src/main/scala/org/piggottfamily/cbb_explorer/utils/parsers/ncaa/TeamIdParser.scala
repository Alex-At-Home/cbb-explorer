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

    val id_regex = "/teams/([0-9]+)".r

    def team_id_finder(e: Element): Option[String] =
      (e >?> element("a.skipMask")).flatMap(e => Option(e.attr("href"))).collect {
        case id_regex(id) => id
      }

    def team_name_finder(e: Element): Option[String] =
      (e >?> element("a.skipMask")).map(_.text)

    val conference_regex = "([A-Za-z].*)".r

    def team_conference_finder(e: Element): Option[String] =
      (e >?> element("td:has(a.skipMask) + td")).map(_.text).collect {
        case conference_regex(conf) => conf
      }
  }

  /** Extract a list of triples for team, NCAA team id, and conference */
  def get_team_triples(filename: String, in: String, adjustFor2020: Boolean = false):
    Either[List[ParseError], List[(TeamId, String, ConferenceId)]] =
  {
    val doc_request_builder = ParseUtils.build_request[Document](`ncaa.get_team_ids`, filename) _
    val browser = JsoupBrowser()

    for {
      doc <- doc_request_builder(browser.parseString(in))
    } yield builders.team_row_finder(doc).flatMap { row =>

      (builders.team_name_finder(row),
        builders.team_id_finder(row),
        builders.team_conference_finder(row)
      ) match {
        case (Some(name), Some(id), Some(conf)) =>
          val adjusted_id = if (adjustFor2020) { //Uses 2019 ids and adjusted based on some random rule I guessed at
            val id_as_int = id.toInt
            val adjustment = (if (id_as_int < 486990) 18820 else 18840)
            val adjusted_id_as_int = id_as_int + adjustment
            s"$adjusted_id_as_int"
          } else {
            id
          }
          List((TeamId(name), adjusted_id, ConferenceId(conf)))
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
