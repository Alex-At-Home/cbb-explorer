package org.piggottfamily.cbb_explorer.utils.parsers.kenpom

import org.piggottfamily.cbb_explorer.models._
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
import scala.util.Try

/** Parses the game HTML (or game subsets of the team HTML) */
trait GameParser {

  import ExtractorUtils._

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  /** This should be the only object that is edited as stats are added to Game */
  protected object game_summary_builders {

    def table_finder(doc: Document): Option[List[Element]] =
      doc >?> elementList("table[id=schedule-table] > tbody > tl")

    val game_model = LabelledGeneric[Game]

    def fields(current_year: Year, eoy_rank: Int) = {
      var f: Game = null  // (just used to infer type in "nameOf")
      Symbol(nameOf(f.opponent)) ->> HtmlExtractor(
        el => el >?> element("a[href^=team.php]"),
        el => Right(TeamSeasonId(TeamId(el.text), current_year))
      ) ::
      Symbol(nameOf(f.date)) ->> HtmlExtractor(
        el => el >?> element("tr td"),
        el => parse_date(el.text, current_year)
      ) ::
      Symbol(nameOf(f.won)) ->> HtmlExtractor(
        el => el >?> element("tr.l"),
        _ => Right(false), //(lost)
        fallback = Some(true) //(won)
      ) ::
      Symbol(nameOf(f.score)) ->> HtmlExtractor(
        el => el >?> element("td:has(b)"),
        el => parse_score(el.text)
      ) ::
      Symbol(nameOf(f.pace)) ->> HtmlExtractor(
        el => el >?> element("td.pace"),
        el => ParseUtils.parse_rank(Some(el.text)) //(lazily re-use the int parser)
      ) ::
      Symbol(nameOf(f.rank)) ->> HtmlExtractor(
        el => el >?> element("span[class=seed-grey]"),
        el => ParseUtils.parse_rank(Some(el.text)),
        fallback = Some(eoy_rank)
      ) ::
      Symbol(nameOf(f.opp_rank)) ->> HtmlExtractor(
        el => el >?> element("span[class=seed]"),
        el => ParseUtils.parse_rank(Some(el.text))
      ) ::
      Symbol(nameOf(f.location_type)) ->> HtmlExtractor(
        el => el >?> element("td:matches(Home|Away|Neutral|Semi-Home|Semi-Away)"),
        el => parse_location_type(el.text)
      ) ::
      Symbol(nameOf(f.tier)) ->> HtmlExtractor(
        el => el >?> element("img"),
        el => parse_tier(el),
        fallback = Some(Game.TierType.C)
      ) ::
      HNil
    }
  }

  private val formatter = DateTimeFormat.forPattern("EEE MMM dd yyyy")

  /** Parse Kenpom format dates eg "Mon Nov 16" into date times */
  protected def parse_date(date_str: String, current_year: Year):
    Either[ParseError, DateTime] =
  {
    //TODO: need to increment year if <June
    Try(formatter.parseDateTime(s"$date_str ${current_year.value}"))
      .map(Right(_.withTime(12, 0, 0, 0)))
      .getOrElse(
        Left(ParseUtils.build_sub_error(nameOf[Game](_.date))(
          s"Unexpected date format: [$date_str]"
        ))
      )
  }

  private val ScoreRegex = "[^WL]*([WL])[^0-9]*([0-9]+)[-]([0-9]+).*".r

  /** Kenpom scores are in the format eg "<b>L</b>, 63-55" */
  protected def parse_score(score_str: String):
    Either[ParseError, Game.Score] = score_str match
  {
    case ScoreRegex(won_str, high_str, low_str) => //(strs are ints by regex construction)
      Right((high_str.toInt, low_str.toInt))
        .right.map {
          case high_low if won_str == "W" => high_low
          case high_low => high_low.swap
        }.right.map {
          (Game.Score.apply _).tupled(_)
        }
      case _ =>
        Left(ParseUtils.build_sub_error(nameOf[Game](_.score))(
          s"Unrecognized score, expecting '[WL], ptsW-ptsL', got: [$score_str]"
        ))
  }

  /** Converts from one of the support locations to the model */
  protected def parse_location_type(location_str: String):
    Either[ParseError, Game.LocationType.Value] = location_str match {
      case "Home" => Right(Game.LocationType.Home)
      case "Away" => Right(Game.LocationType.Away)
      case "Neutral" => Right(Game.LocationType.Neutral)
      case "Semi-Home" => Right(Game.LocationType.SemiHome)
      case "Semi-Away" => Right(Game.LocationType.SemiAway)
      case _ =>
        Left(ParseUtils.build_sub_error(nameOf[Game](_.location_type))(
          s"Unrecognized location type: [$location_str]"
        ))
    }

  /** Get the Kenpom tier for the game (want to add a tier D for terrible opponents at some point) */
  protected def parse_tier(element: Element):
    Either[ParseError, Game.TierType.Value] =
  {
    element.attrs.get("src") match {
      case Some("https://kenpom.com/assets/a.gif") =>
        Right(Game.TierType.A)
      case Some("https://kenpom.com/assets/b.gif") =>
        Right(Game.TierType.B)
      case _ =>
        Left(ParseUtils.build_sub_error(nameOf[Game](_.tier))(
          s"Unrecognized tier element: [${element.outerHtml}]"
        ))
    }
  }

  /**
   * Parses HTML fragment representing a team's games
   * //TODO parse with warnings instead of error'ing out?!
   */
  def parse_games(doc: Document, current_year: Year, eoy_rank: Int):
    Either[List[ParseError], List[Game]] =
  {
    game_summary_builders.table_finder(doc).map { rows =>
      val fields = game_summary_builders.fields(current_year, eoy_rank)
      val games_or_errors = rows.map { row =>
        object games_extractor extends HtmlExtractorMapper {
          override val root = row
        }
        //TODO: extract team name first so we can use it in errors
        ParseUtils.sequence_kv_results(fields map games_extractor).right.map(
          game_summary_builders.game_model.from(_)
        )
      } //returns List[Either[List[ParseError, Game]]]

      games_or_errors.parSequence // returns Either[List[ParseError], List[Game]]

    }.getOrElse(
      Left(List(ParseUtils.build_sub_error(`parent_fills_in`)(
        s"Could not find game table"
      )))
    )
  }
}
object GameParser extends GameParser
