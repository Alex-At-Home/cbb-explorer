package org.piggottfamily.cbb_explorer.utils.parsers.play_types

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
trait GameParser {

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `play_types.parse_game` = "play_types.parse_game"

  // Holds all the HTML parsing logic
  protected object builders {

    def team_names_finder(doc: Document): List[String] =
      (doc >?> elementList("table.Tier:has(td:contains(Final)) tr+tr td.TierData")) //"tr+tr" == 2nd+ rows
        .getOrElse(Nil).map(_.text)

    def possessions_finder(doc: Document): List[Element] =
      (doc >?> elementList("td.PlayByPlayRowShaded, td.PlayByPlayRow"))
      .getOrElse(Nil)

    def play_finder(el: Element): String = "" //TODO
  }



}
