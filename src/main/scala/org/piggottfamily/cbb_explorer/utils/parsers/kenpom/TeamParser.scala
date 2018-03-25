package org.piggottfamily.cbb_explorer.utils.parsers.kenpom

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.utils.parsers._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._
import io.circe._, io.circe.parser._

/** Parses the team HTML */
trait TeamParser {

  protected val `kenpom.parse_team` = "kenpom.parse_team"
  protected val `kenpom.parse_team.parse_game` = "kenpom.parse_team.parse_game"

  /**
   * Parses HTML representing a team's season
   */
  def parse_team(in: String, filename: String): Either[List[ParseError], ParseResponse[TeamSeason]] = {

    val browser = JsoupBrowser()

    // Error reporters:
    val doc_request_builder = ParseUtils.build_request[Document](`kenpom.parse_team`, filename) _
    val single_error_enricher = ParseUtils.enrich_sub_error(`kenpom.parse_team`, filename) _
    val multi_error_enricher = ParseUtils.enrich_sub_errors(`kenpom.parse_team`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      // Get team name and year
      team_season <- parse_filename(filename).left.map(multi_error_enricher)

      // Get coach
      coach_or_errors = parse_coach(doc).left.map(single_error_enricher)

      // Get basic ranking and metrics
      metrics_or_errors = parse_metrics(doc).left.map(multi_error_enricher)

      // TODO Get games
      // TODO Get players

      coach_metrics <- ParseUtils.lift(coach_or_errors, metrics_or_errors)
      (coach, metrics) = coach_metrics //SI-5589

    } yield ParseResponse(TeamSeason(
      team_season, metrics, games = Nil, players = Map.empty, coach
    ))
  }

  /** Extracts the team name and year from the filename */
  protected def parse_filename(filename: String): Either[List[ParseError], TeamSeasonId] = {
    /** The filename spat out by webhttrack */
    val FilenameRegex = "team[0-9a-f]{4}(20[0-9]{2})?_([^_]+)?.*[.]html".r

    filename match {
      case FilenameRegex(year, team_name) =>
        val errors = List("year" -> year, "team_name" -> team_name).collect {
          case (fieldname, value) if Option(value).isEmpty =>
            ParseUtils.build_sub_error(fieldname)(
              s"Failed to parse [$filename] - couldn't extract [$fieldname]"
            )
        }
        errors match {
          case Nil => Right(TeamSeasonId(TeamId(team_name), Year(year.toInt)))
          case _ => Left(errors)
        }
      case _ =>
        Left(List(ParseUtils.build_sub_error("")(
          s"Completely failed to parse [$filename] to extract year and team"
        )))
    }
  }

  /** Parses out the coach id from the top level HTML */
  protected def parse_coach(doc: Document): Either[ParseError, CoachId] = {
    (doc >?> element("span[class=coach]") >?> element("a")).flatten match {
      case Some(coach) =>
        Right(CoachId(coach.text))
      case None =>
        Left(ParseUtils.build_sub_error("coach")(
          s"Failed to parse HTML - couldn't extract [coach]"
        ))
    }
  }

  /** Parses out the season and conference metrics from various spots in the HTML
      and (ugh) script functions
  */
  protected def parse_metrics(doc: Document): Either[List[ParseError], TeamSeasonStats] = {

    val root: Either[List[ParseError], Unit] = Right(())
    def single_error_enricher(field: String) = ParseUtils.enrich_sub_error("", field) _

    for {
      _ <- root

      adj_margin_rank_or_error = parse_rank(
        (doc >?> element("div[id=title-container]") >?> element("span[class=rank]")).flatten
      ).left.map(single_error_enricher("adj_margin_rank"))

      season_stats_table_or_error = create_table(doc, "function tableStart", "function")

      conf_stats_table_or_error = create_table(doc, "if (checked)", "function")

      table_tuple <- ParseUtils.lift(season_stats_table_or_error, conf_stats_table_or_error)
      (season_stats_table, conf_stats_table) = table_tuple

      season_stats_or_error = parse_season_stats(season_stats_table)

      metrics_tuple <- ParseUtils.lift(adj_margin_rank_or_error, season_stats_or_error)
      (adj_margin_rank, season_stats) = metrics_tuple

      adj_margin_value = season_stats.adj_off.value - season_stats.adj_def.value

    } yield season_stats.copy(
      adj_margin = Metric(adj_margin_value, adj_margin_rank)
    )
  }

  /** Gets the metric and ranking off the HTML fragment from the script */
  protected def parse_season_stats(in: Map[String, Either[ParseError, Document]]):
    Either[List[ParseError], TeamSeasonStats] =
  for {

    adj_off_or_error = get_metric_and_rank("td#OE")
    adj_def_or_error = get_metric_and_rank("td#DE")

    adj_off_def = ParseUtils.lift(adj_off_or_error, adj_def_or_error)
    (adj_off, adj_def) = adj_off_def

  } yield TeamSeasonStats(adj_margin = Metric(0.0, 0), adj_off, adj_def)

  protected def get_metric_and_rank(sub_doc: Option[Document]): Either[List[ParseError], Metric] = sub_doc match {
    case Some(html) => for {

      score_or_error = (html >?> element("a")).headOption { parse_score(_) }
      rank_or_error = (html >?> element("span[class=seed]")).headOption { parse_rank(_) }

      score_rank = ParseUtils.lift(score_or_error, rank_or_error)
      (score, rank) = score_rank

    } yield Metric(score, rank)

    case None =>
      Left(ParseUtils.build_sub_error("value")(
        s"Failed to locate the field"
      ))
  }

  /** Parses out a score (double) from HTML */
  protected def parse_score(el: Option[Element]): Either[List[ParseError], Double] = el match {
    case Some(score) =>
      ParseUtils.build_request[Document]("", "")(score.text.toDouble)
    case None =>
      Left(ParseUtils.build_sub_error("value")(
        s"Failed to locate the field"
      ))
  }

  /** Parses out a rank from HTML */
  protected def parse_rank(el: Option[Element]): Either[List[ParseError], Int] = el match {
    case Some(rank) =>
      ParseUtils.build_request[Document]("", "")(rank.text.toInt)
    case None =>
      Left(ParseUtils.build_sub_error("rank")(
        s"Failed to locate the field"
      ))
  }

  /** All the stats are created by a JS function that inserts different HTML
      depending on whether the "conf" vs "season" checkbox is checked
      This function locates the relevant portion of the script section in the HTML
      and parses all the metrics into a large map
  */
  protected def parse_stats_table(doc: Document, start_token: String, end_token: String):
    Either[ParseError, Map[String, Either[ParseError, Document]]] =
  {
    val browser = JsoupBrowser()

    (doc >> elementList("script")).collect {
      case script Right script.contains(start_token) =>
        script.text
    }.headOption match {
      case Some(script) => for {
        start <- parse_string_offset(script, start_token, "start_token")
        sub_str1 = script.substring(start)
        end <- parse_string_offset(script, end_token, "end_token")
      } yield parse_script_function(sub_str1.substring(0, end))

      case None =>
        Left(ParseUtils.build_sub_error("")(
          s"Failed to locate script containing token [$start_token]"
        ))
    }
  }

  /** Utility for building sub-strings */
  protected def parse_string_offset(in: String, token: String, token_type: String):
    Either[ParseError, Int] = for {
      sub_str = Right(in.indexOf(in)).flatMap {
        case valid if valid >= 0 => Right(valid)
        case invalid =>
          Left(ParseUtils.build_sub_error(token_type)(
            s"Failed to locate [$token] at [$token_type] in [$in]"
          ))
      }
    }

  /** Parses a script function containing all the efficiency values into
      a map of the HTML, example:
      $("td#ARate").html("<a href=\"teamstats.php?s=RankARate\">54.9</a> <span class=\"seed\">6</span>");
  */
  protected def parse_script_function(in: String): Map[String, Either[List[ParseError], Document]] = {
    val HtmlRegex = """[^$]*[$][(]"([^"]+)"[)][.]html[(]"(.*)"[)];[^;]*""".r
    in.lines.collect {
      case HtmlRegex(element_id, html) =>
        val map_value = parse(html).asString match {
          case Some(html_str) =>
            ParseUtils.build_request[Document]("", element_id)(browser.parseString(html_str))
          case None =>
            Left(ParseUtils.build_sub_error(element_id)(
              s"Failed to create HTML for [$element_id] - input was [$html_str]"
            ))
        }
        element_id -> map_value
    }.toMap
  }

  /**
   * Parses HTML fragment representing a team's games
   */
  protected def parse_games(): Either[List[ParseError], ParseResponse[List[Game]]] = {
    Left(Nil) //TODO
  }
  /**
   * Parses HTML fragment representing a single game
   */
  protected def parse_game(): Either[List[ParseError], Game] = {
    //   case class Game(
    //     opponent: TeamSeasonId,
    //     won: Boolean,
    //     rank: Int,
    //     opp_rank: Int,
    //     location_type: Game.LocationType.Value,
    //     tier: Game.PrimaryTier.Value,
    //     secondary_tiers: Set[Game.SecondaryTier.Value]
    //   )
    //

    // TODO
    // Get opponent
    // Get result
    // Get my ranking
    // Get opponent ranking
    // Get location type
    // Get primary tier
    // Get secondary tiers
    Left(Nil)
  }
}
object TeamParser extends TeamParser
