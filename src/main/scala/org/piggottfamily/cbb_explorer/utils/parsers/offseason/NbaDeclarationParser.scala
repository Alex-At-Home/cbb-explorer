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
         builders.early_declaration_finder(doc) ++ builders.senior_declaration_finder(doc)
      }

      name_team_pairs = names.flatMap { csv =>
         csv.split(" *, *").toList match {
            case List(name, pos, team_extractor(team)) =>
               List((name, team))   

            case _ => //(can't format)
               List()
         }
      }
    } yield name_team_pairs
  }
}
object NbaDeclarationParser extends NbaDeclarationParser

