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
  protected val `ncaa.parse_player` = "ncaa.parse_player"

  // Holds all the HTML parsing logic
  trait base_builders {
    def coach_finder(doc: Document): Option[String]
    def player_info_finder(doc: Document): Option[List[Element]]
    def name_finder(el: Element): Option[String]
    def number_finder(el: Element): Option[String]
    def ncaa_id_finder(el: Element): Option[String]
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

    def ncaa_id_finder(el: Element): Option[String] =
      (el >?> element("td:eq(1) > a"))
        .map(_.attr("href"))
        .map(
          _.split("stats_player_seq=").last
        ) // (split guaranteed to have 1 element)

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

    def origin_finder(el: Element): Option[String] =
      None // (not supported in v0)
  }
  protected object builders_v1 extends base_builders {

    def coach_finder(doc: Document): Option[String] =
      (doc >?> element(
        "div.card-header:contains(Coach) + div.card-body a[href]"
      )).map(_.text)

    def player_info_finder(doc: Document): Option[List[Element]] =
      (doc >?> elementList("table.dataTable tbody tr"))

    def name_finder(el: Element): Option[String] =
      (el >?> element("td:eq(3)")).map(_.text)

    def ncaa_id_finder(el: Element): Option[String] =
      (el >?> element("td:eq(3) > a"))
        .map(_.attr("href"))
        .map(_.split("/").last) // (split guaranteed to have 1 element)

    def number_finder(el: Element): Option[String] =
      (el >?> element("td:eq(2)")).map(_.text)

    def pos_finder(el: Element): Option[String] =
      (el >?> element("td:eq(5)")).map(_.text)

    def height_finder(el: Element): Option[String] =
      (el >?> element("td:eq(6)")).map(_.text)

    def class_finder(el: Element): Option[String] =
      (el >?> element("td:eq(4)")).map(
        _.text.replace(".", "")
      ) // (remove trailing ".")

    def games_played_finder(el: Element): Option[String] =
      (el >?> element("td:eq(0)")).map(_.text)

    def origin_finder(el: Element): Option[String] =
      (el >?> element("td:eq(7)")).map(_.text)

    def player_unified_ncaa_id_finder(doc: Document): List[String] =
      (doc >?> elementList("tr[id^=player_season_] td:first-child a"))
        .map { els =>
          els.flatMap { el =>
            val href = el.attr("href") // "/players/7033318"
            href.split("/").lastOption // "7033318"
          }
        }
        .getOrElse(Nil)
  }
  protected def builders_array = Array(builders_v0, builders_v1)

  /** Gets bonus player info from the HTML page */
  def get_unified_ncaa_id(
      filename: String,
      in: String
  ): Either[List[ParseError], Option[String]] = {
    val browser = JsoupBrowser()

    val doc_request_builder =
      ParseUtils.build_request[Document](`ncaa.parse_player`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))
      ncaa_ids = builders_v1.player_unified_ncaa_id_finder(doc)
    } yield ncaa_ids
      .sortBy(_.toInt)
      .headOption
      .map(_.toString) // pick the lowest numberical value
  }

  /** Gets the boxscore lineup from the HTML page */
  def parse_roster(
      filename: String,
      in: String,
      team_id: TeamId,
      version_format: Int,
      include_coach: Boolean = false
  ): Either[List[ParseError], List[RosterEntry]] = {
    val browser = JsoupBrowser()
    val builders = builders_array(version_format)

    // Error reporters
    val doc_request_builder =
      ParseUtils.build_request[Document](`ncaa.parse_roster`, filename) _
    val single_error_completer =
      ParseUtils.enrich_sub_error(`ncaa.parse_roster`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      coach <- Right(
        builders
          .coach_finder(doc)
          .filter(_ => include_coach)
          .map { coach_name =>
            RosterEntry(
              LineupEvent.PlayerCodeId("__coach__", PlayerId(coach_name)),
              "",
              "",
              "",
              None,
              "",
              -1,
              None,
              None
            )
          }
      )
      players <- Right(
        builders
          .player_info_finder(doc)
          .getOrElse(Nil)
          .flatMap { el =>
            (for {
              name <- builders.name_finder(el).map {
                case v0_formatted_name if version_format == 0 =>
                  v0_formatted_name
                case v1_formatted_name =>
                  ExtractorUtils.name_in_v0_box_format(v1_formatted_name)
              }

              // No initials allowed in the roster:
              _ <- if (name_is_initials(name).nonEmpty) None else Some(())

              maybe_ncaa_id = builders.ncaa_id_finder(el)

              player_code_id = build_player_code( // (fixes accent and misspellings)
                name,
                Some(team_id)
              ).copy(
                ncaa_id = maybe_ncaa_id
              )
              number <- builders.number_finder(el)
              pos <- builders.pos_finder(el)
              height <- builders.height_finder(el)
              year_class <- builders.class_finder(el)
              gp <- builders.games_played_finder(el)

              height_in = height match {
                case RosterEntry.height_regex(ft, in) =>
                  Some(ft.toInt * 12 + in.toInt)
                case _ => None
              }
              origin = builders.origin_finder(el)

            } yield RosterEntry(
              player_code_id,
              number,
              pos,
              height,
              height_in,
              year_class,
              Try(gp.toInt).getOrElse(0),
              origin,
              None
            )).toList
          }
          .sortWith { // So below we dedup the smaller number of games played
            case (lhs, rhs) =>
              lhs.gp > rhs.gp || ((lhs.gp == rhs.gp) && (lhs.player_code_id.code < rhs.player_code_id.code))
          }
          .foldLeft(Map[LineupEvent.PlayerCodeId, RosterEntry]()) { (acc, v) =>
            val player_code_with_no_ncaa_id =
              v.player_code_id.copy(ncaa_id = None)
            acc.get(player_code_with_no_ncaa_id) match {
              // Can get duplicate names so just ignore them
              case Some(_) => acc
              case None    => acc + (player_code_with_no_ncaa_id -> v)
            }
          }
          .values
          .toList
          .sortWith(_.gp > _.gp)
      )

      // Validate duplicates (like in box score parsing logic):
      player_codes = players.map(p => p.player_code_id.code)
      _ <-
        if (player_codes.toSet.size != players.size) {
          val playersByCode = players
            .groupBy(_.player_code_id.code)
            .mapValues(_.map(_.player_code_id).reverse)
          if (include_coach) {
            // Add some more
            playersByCode.foreach {
              case (_, all_dup_players @ (dup_player +: other_dup_players))
                  if other_dup_players.size == 1 =>
                val first_last =
                  ExtractorUtils.v0_box_name_to_first_last(dup_player.id.name)
                println(
                  s"""TOFIX1: fix_combos("${first_last._1}", "${first_last._2}") ++"""
                )
                all_dup_players.foreach { all_dup_player =>
                  val first_last_all =
                    ExtractorUtils.v0_box_name_to_first_last(
                      all_dup_player.id.name
                    )
                  println(
                    s"""TOFIX2: fix_combos("${first_last_all._1}", "${first_last_all._2}", Some("??")) ++"""
                  )
                }
              case _ =>
              // (can't fix this case or don't need to)
            }
          }
          Left(
            List(
              ParseUtils.build_sub_error(`parent_fills_in`)(
                s"Duplicate players: [${players.groupBy(_.player_code_id.code).mapValues(_.map(_.player_code_id)).partition(_._2.size > 1)}]"
              )
            )
          )

        } else {
          Right(())
        }

    } yield players ++ coach.toList
  }

  // Utils

}
object RosterParser extends RosterParser
