package org.piggottfamily.cbb_explorer.utils.parsers.offseason

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.ExtractorUtils._
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
import org.jsoup.Jsoup
import scala.collection.JavaConverters._

trait NbaDeclarationParser {
  
  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `offseason.parse_nba_declarations` = "offseason.parse_nba_declarations"

  // Holds all the HTML parsing logic
  protected object builders {

   def early_declaration_finder(doc: Document): List[String] = 
      (doc >?> elementList("h2:contains(college underclassmen) + p + ol li")).map(els => els.map(_.text)).getOrElse(List())

   def senior_declaration_finder(doc: Document): List[String] = 
      (doc >?> elementList("h2:contains(college seniors) + p + ol li")).map(els => els.map(_.text)).getOrElse(List())

   def early_declaration_finder_2020(doc: Document): List[String] = 
      (doc >?> elementList("h3:contains(college players) + p + ol li")).map(els => els.map(_.text)).getOrElse(List())

    /** Hoops Rumors ~2025+ uses {@code h3} plus multiple {@code ol} blocks (e.g. "Expected to remain"
      * vs "Testing the draft waters") before {@code h3} International. Older pages used {@code h2}
      * plus a single {@code h2 + p + ol} chain; some years had separate college seniors {@code h2}.
      * Collect every {@code ol > li} between the college-underclassmen heading and the international heading.
      */
    def college_entrant_li_from_entry_content(html: String): List[String] = {
      val doc = Jsoup.parse(html)
      val entry = doc.selectFirst("div.entry-content")
      if (entry == null) return Nil
      val kids = entry.children.asScala.toVector
      val startIdx = kids.indexWhere { el =>
        val tag = el.tagName.toLowerCase
        (tag == "h2" || tag == "h3") && {
          val t = el.text.trim.toLowerCase
          t.contains("college") && t.contains("underclassmen")
        }
      }
      if (startIdx < 0) return Nil
      val stopIdx = kids.indexWhere(
        { el =>
          val tag = el.tagName.toLowerCase
          (tag == "h2" || tag == "h3") && el.text.trim.toLowerCase.contains("international")
        },
        startIdx + 1
      )
      val endIdx = if (stopIdx < 0) kids.length else stopIdx
      kids
        .slice(startIdx + 1, endIdx)
        .flatMap { el =>
          if (el.tagName.equalsIgnoreCase("ol"))
            el.select("li").asScala.map(_.text.trim.replaceAll("\\s+", " ")).toList
          else Nil
        }
        .toList
    }

  }

  /** Output format player / team */
  def get_declarations(filename: String, in: String): Either[List[ParseError], List[(String, String)]] = {

    val browser = JsoupBrowser()

    // Error reporters
    val doc_request_builder = ParseUtils.build_request[Document](`offseason.parse_nba_declarations`, filename) _
    val single_error_completer = ParseUtils.enrich_sub_error(`offseason.parse_nba_declarations`, filename) _


    //TODO: note this will mis-extract some team names , eg Miami (FL) - gets handled in the team name normalizer
    val team_extractor = "^([^(]+)(?: [(].*)?$".r //<<team name>> (<<class-if-underclassman>>) 

    for {
      doc <- doc_request_builder(browser.parseString(in))

      names = if (filename.contains("2020")) {
         builders.early_declaration_finder_2020(doc) 
      } else {
        val modern = builders.college_entrant_li_from_entry_content(in)
        if (modern.nonEmpty) modern
        else
          builders.early_declaration_finder(doc) ++ builders.senior_declaration_finder(doc)
      }

      name_team_pairs = names.flatMap { csv =>
         csv.split(" *, *").toList match {
            case List(name, pos, team_extractor(team)) =>
               List((name.trim, team.trim))

            case _ => //(can't format)
               List()
         }
      }
    } yield name_team_pairs
  }
}
object NbaDeclarationParser extends NbaDeclarationParser

