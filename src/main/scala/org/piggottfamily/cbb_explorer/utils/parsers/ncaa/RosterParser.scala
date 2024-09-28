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

/** Parses the game HTML (or game subsets of the team HTML) */
trait RosterParser {

  import ExtractorUtils._

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `ncaa.parse_roster` = "ncaa.parse_roster"

  // Holds all the HTML parsing logic
  trait base_builders {
    def coach_finder(doc: Document): Option[String]
    def player_info_finder(doc: Document): Option[List[Element]]
    def name_finder(el: Element): Option[String]
    def number_finder(el: Element): Option[String]
    def pos_finder(el: Element): Option[String]
    def height_finder(el: Element): Option[String]
    def class_finder(el: Element): Option[String]
    def games_played_finder(el: Element): Option[String]
    def origin_finder(el: Element): Option[String]
  }
  protected object builders_v0 extends base_builders {

    def coach_finder(doc: Document): Option[String] =
      (doc >?> element("div#head_coaches_div a[href]")).map(_.text)

    def player_info_finder(doc: Document): Option[List[Element]] =
      (doc >?> elementList("table#stat_grid tbody tr"))

    def name_finder(el: Element): Option[String] =
      (el >?> element("td:eq(1)")).map(_.text)

    def number_finder(el: Element): Option[String] =
      (el >?> element("td:eq(0)")).map(_.text)

    def pos_finder(el: Element): Option[String] =
      (el >?> element("td:eq(2)")).map(_.text)

    def height_finder(el: Element): Option[String] =
      (el >?> element("td:eq(3)")).map(_.text)

    def class_finder(el: Element): Option[String] =
      (el >?> element("td:eq(4)")).map(_.text)

    def games_played_finder(el: Element): Option[String] =
      (el >?> element("td:eq(5)")).map(_.text)

    def origin_finder(el: Element): Option[String] = None //(not supported in v0)
  }
  protected object builders_v1 extends base_builders {

    def coach_finder(doc: Document): Option[String] =
      (doc >?> element("div.card-header:contains(Coach) + div.card-body a[href]")).map(_.text)

    def player_info_finder(doc: Document): Option[List[Element]] =
      (doc >?> elementList("table.dataTable tbody tr"))

    def name_finder(el: Element): Option[String] =
      (el >?> element("td:eq(3)")).map(_.text)

    def number_finder(el: Element): Option[String] =
      (el >?> element("td:eq(2)")).map(_.text)

    def pos_finder(el: Element): Option[String] =
      (el >?> element("td:eq(5)")).map(_.text)

    def height_finder(el: Element): Option[String] =
      (el >?> element("td:eq(6)")).map(_.text)

    def class_finder(el: Element): Option[String] =
      (el >?> element("td:eq(4)")).map(_.text)

    def games_played_finder(el: Element): Option[String] =
      (el >?> element("td:eq(0)")).map(_.text)

    def origin_finder(el: Element): Option[String] =
      (el >?> element("td:eq(7)")).map(_.text)
  }
  protected def builders_array = Array(builders_v0, builders_v1)

  /** Gets the boxscore lineup from the HTML page */
  def parse_roster(
    filename: String, in: String, team_id: TeamId, version_format: Int, include_coach: Boolean = false
  ): Either[List[ParseError], List[RosterEntry]] =
  {
    val browser = JsoupBrowser()
    val builders = builders_array(version_format)

    // Error reporters
    val doc_request_builder = ParseUtils.build_request[Document](`ncaa.parse_roster`, filename) _
    val single_error_completer = ParseUtils.enrich_sub_error(`ncaa.parse_roster`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      coach <- Right(
        builders.coach_finder(doc).filter(_ => include_coach).map { coach_name =>
          RosterEntry(
            LineupEvent.PlayerCodeId("__coach__", PlayerId(coach_name)), "", "", "", None, "", -1, None
          )
        }
      )

      players <- Right(
        builders.player_info_finder(doc).getOrElse(Nil).flatMap { el =>
          (for {
            name <- builders.name_finder(el)

            // No initials allowed in the roster:
            _ <- if (name_is_initials(name).nonEmpty) None else Some(())

            player_code_id = build_player_code(name, Some(team_id)) //(fixes accent and misspellings)
            number <- builders.number_finder(el)
            pos <- builders.pos_finder(el)
            height <- builders.height_finder(el)
            year_class <- builders.class_finder(el)
            gp <- builders.games_played_finder(el)

            height_in = height match {
              case RosterEntry.height_regex(ft, in) => Some(ft.toInt*12 + in.toInt)
              case _ => None
            }
            origin = builders.origin_finder(el)

          } yield RosterEntry(
            player_code_id, number, pos, height, height_in, year_class, Try(gp.toInt).getOrElse(0), origin
          )).toList
        }.sortWith { // So below we dedup the smaller number of games played
          case (lhs, rhs) => 
            lhs.gp > rhs.gp || ((lhs.gp == rhs.gp) && (lhs.player_code_id.code < rhs.player_code_id.code))
        }.foldLeft(Map[LineupEvent.PlayerCodeId, RosterEntry]()) { (acc, v) => acc.get(v.player_code_id) match {
          // Can get duplicate names so just
          case  Some(_) => acc
          case None => acc + (v.player_code_id -> v)
        }}.values.toList.sortWith(_.gp > _.gp)
      )

      // Validate duplicates (like in box score parsing logic):
      player_codes = players.map(p => p.player_code_id.code)
      _ <- if (player_codes.toSet.size != players.size) {
        Left(List(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Duplicate players: [${players.groupBy(_.player_code_id.code).mapValues(_.map(_.player_code_id)).partition(_._2.size > 1)}]"
        )))

      } else {
        Right(())
      }

    } yield players ++ coach.toList
  }

  // Utils

}
object RosterParser extends RosterParser
