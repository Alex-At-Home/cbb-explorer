package org.piggottfamily.cbb_explorer.utils.parsers.offseason

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.offseason._
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

trait RecruitParser {
  
  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `offseason.parse_recruits` = "offseason.parse_recruits"

  // Holds all the HTML parsing logic
  protected object builders {

   def player_finder(doc: Document): List[Element] =
      (doc >?> elementList("li.rankings-page__list-item")).getOrElse(List())

   def get_name(el: Element): Option[String] = 
      (el >?> element("a.rankings-page__name-link")).map(_.text).filter(_.nonEmpty)

   def get_pos(el: Element): Option[String] = 
      (el >?> element("div.position")).map(_.text).filter(_.nonEmpty)

   def get_height(el: Element): Option[String] =  //format ft-in
      (el >?> element("div.metrics")).map(_.text).filter(_.nonEmpty).map(_.split(" */ *")(0))

   def get_rating(el: Element): Option[String] = 
      (el >?> element("span.score")).map(_.text).filter(_.nonEmpty)

   def get_destination(el: Element): Option[String] = 
      (el >?> element("div.status b.checkmark")).map(_.parent) //checkmark means has committed
         .flatMap(_ >?> element("img")).flatten.map(_.attr("alt"))

   def get_destination_2022(el: Element): Option[String] = 
      (el >?> element("div.status img")).map(_.attr("alt")).filter(_.nonEmpty)

  }

  /** Output format player / team */
  def get_early_declarations(filename: String, in: String): Either[List[ParseError], List[Recruit]] = {

    val browser = JsoupBrowser()

    // Error reporters
    val doc_request_builder = ParseUtils.build_request[Document](`offseason.parse_recruits`, filename) _
    val single_error_completer = ParseUtils.enrich_sub_error(`offseason.parse_recruits`, filename) _

    val height_regex = "([0-9])[-]([0-9]+)".r

    for {
      doc <- doc_request_builder(browser.parseString(in))

      players = builders.player_finder(doc)

      recruits = players.flatMap { player =>

         val parsed_params = (
            builders.get_name(player), 
            builders.get_pos(player), 
            builders.get_height(player), 
            builders.get_rating(player), 
            if (filename.contains("2022")) builders.get_destination_2022(player) else builders.get_destination(player)
               //(current year be less needy, and count a player even if they don't have green checkmark, will fix later)
         ) 

         // Handy output:
         println(parsed_params)

         parsed_params.mapN { 
            case (name, pos, height_regex(ft, in), rating, dest) if Try { rating.toDouble }.isSuccess =>
               Some(Recruit(name, pos, ft.toInt*12 + in.toInt, rating.toDouble, dest, profile = None))

            case _ => None
         }.toList.flatten
      }

    } yield recruits
  }
}
object RecruitParser extends RecruitParser

