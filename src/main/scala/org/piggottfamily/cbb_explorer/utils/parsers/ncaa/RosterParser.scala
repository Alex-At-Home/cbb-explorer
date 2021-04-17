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
  protected object builders {

    def player_info_finder(doc: Document): Option[List[Element]] =
      (doc >?> elementList("table#stat_grid tbody tr"))

    def name_finder(el: Element): Option[String] =
      (el >?> element("td:eq(1)")).map(_.text)

    def number_finder(el: Element): Option[String] =
      (el >?> element("td:eq(0)")).map(_.text)

    def height_finder(el: Element): Option[String] =
      (el >?> element("td:eq(3)")).map(_.text)

    def class_finder(el: Element): Option[String] =
      (el >?> element("td:eq(4)")).map(_.text)
  }

  /** Gets the boxscore lineup from the HTML page */
  def parse_roster(
    filename: String, in: String, team_id: TeamId
  ): Either[List[ParseError], List[RosterEntry]] =
  {
    val browser = JsoupBrowser()

    // Error reporters
    val doc_request_builder = ParseUtils.build_request[Document](`ncaa.parse_roster`, filename) _
    val single_error_completer = ParseUtils.enrich_sub_error(`ncaa.parse_roster`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      players <- Right(
        builders.player_info_finder(doc).getOrElse(Nil).flatMap { el =>
          (for {
            name <- builders.name_finder(el)
            player_id = build_player_code(name, Some(team_id))
            number <- builders.number_finder(el)
            height <- builders.height_finder(el)
            year_class <- builders.class_finder(el)
          } yield RosterEntry(
            player_id, number, height, year_class
          )).toList
        }
      )

      // Validate duplicates (like in box score parsing logic):
      player_codes = players.map(_.player_id.code)
      _ <- if (player_codes.toSet.size != players.size) {
        Left(List(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Duplicate players: [$player_codes]"
        )))

      } else {
        Right(())
      }

    } yield players
  }

  // Utils

}
object RosterParser extends RosterParser
