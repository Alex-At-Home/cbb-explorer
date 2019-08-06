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
trait TeamScheduleParser {

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `ncaa.get_neutral_games` = "ncaa.get_neutral_games"

  // Holds all the HTML parsing logic
  protected object builders {
    def neutral_game_finder(doc: Document): List[String] =
      (doc >?> elementList(
        "legend:contains(Schedule/Results) + table " +
        "tr:has(td:matches(.*[@][a-zA-Z]+.*)) > td:matches([0-9]+/[0-9]+/[0-9]+)"
      )).getOrElse(Nil).map(_.text)
  }

  /** Gets a list of neutral game dates from the team schedule */
  def get_neutral_games(filename: String, in: String): Either[List[ParseError], Set[String]] = {

    val browser = JsoupBrowser()

    // Error reporters
    val doc_request_builder = ParseUtils.build_request[Document](`ncaa.get_neutral_games`, filename) _
    val single_error_completer = ParseUtils.enrich_sub_error(`ncaa.get_neutral_games`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      candidate_neutral_games = builders.neutral_game_finder(doc)

    } yield candidate_neutral_games.toSet //TODO collect on regex
  }

  import ExtractorUtils._
}
object TeamScheduleParser extends TeamScheduleParser
