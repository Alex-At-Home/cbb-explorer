package org.piggottfamily.cbb_explorer.utils.parsers.kenpom

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.utils.parsers._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._
import io.circe._, io.circe.parser._
import cats.implicits._
import cats.data._


/** Parses the team HTML */
trait TeamParser {

  protected val `kenpom.parse_team` = "kenpom.parse_team"
  protected val `kenpom.parse_team.parse_game` = "kenpom.parse_team.parse_game"

  /**
   * Parses HTML representing a team's season
   */
  def parse_team(in: String, filename: String, default_year: Year)
    : Either[List[ParseError], ParseResponse[TeamSeason]] =
  {
    val browser = JsoupBrowser()

    // Error reporters:
    val doc_request_builder = ParseUtils.build_request[Document](`kenpom.parse_team`, filename) _
    val single_error_enricher = ParseUtils.enrich_sub_error(`kenpom.parse_team`, filename) _
    val multi_error_enricher = ParseUtils.enrich_sub_errors(`kenpom.parse_team`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      // Get team name and year
      team_season <- parse_filename(filename, default_year).left.map(multi_error_enricher)

      // Get coach
      coach_or_errors = parse_coach(doc).left.map(single_error_enricher)

      // Get conference
      conf_or_errors = parse_conf(doc).left.map(single_error_enricher)

      // Get basic ranking and metrics
      metrics_or_errors = parse_metrics(doc).left.map(multi_error_enricher)

      // TODO Get games
      // TODO Get players

      various_fields <- (
        coach_or_errors, conf_or_errors, metrics_or_errors
      ).parMapN((_, _, _))
      (coach, conf, metrics) = various_fields //SI-5589

    } yield ParseResponse(TeamSeason(
      team_season, metrics, games = Nil, players = Map.empty, coach, conf
    ))
  }

  /** Extracts the team name and year from the filename */
  protected def parse_filename(filename: String, default_year: Year)
    : Either[List[ParseError], TeamSeasonId] =
  {
    /** The filename spat out by webhttrack */
    val FilenameRegex = "team[0-9a-f]{4}(20[0-9]{2})?_([^_]+)?.*[.]html".r

    filename match {
      case FilenameRegex(maybe_year, team_name) =>
        val year = Option(maybe_year).map(_.toInt).getOrElse(default_year.value)
        val errors = List("year" -> year, "team_name" -> team_name).collect {
          case (fieldname, value) if Option(value).isEmpty =>
            ParseUtils.build_sub_error(fieldname)(
              s"Failed to parse [$filename] - couldn't extract [$fieldname]"
            )
        }
        errors match {
          case Nil => Right(TeamSeasonId(TeamId(team_name), Year(year)))
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

  /** Parses out the coach id from the top level HTML */
  protected def parse_conf(doc: Document): Either[ParseError, ConferenceId] = {
    (doc >?> element("span[class=otherinfo]") >?> element("a")).flatten match {
      case Some(conference) =>
        Right(ConferenceId(conference.text))
      case None =>
        Left(ParseUtils.build_sub_error("conference")(
          s"Failed to parse HTML - couldn't extract [conference]"
        ))
    }
  }

  /** Parses out the season and conference metrics from various spots in the HTML
      and (ugh) script functions
  */
  protected def parse_metrics(doc: Document): Either[List[ParseError], TeamSeasonStats] = {

    val root: Either[List[ParseError], Unit] = Right(())
    def single_error_enricher(field: String) = ParseUtils.enrich_sub_error("", field) _
    def multi_error_enricher(field: String) = ParseUtils.enrich_sub_errors("", field) _

    def get_stats(doc: Document, start_token: String, end_token: String) = {
      parse_stats_table(doc, start_token, end_token).map(parse_script_function)
    }

    for {
      _ <- root

      adj_margin_rank_or_error = ParseUtils.parse_rank(
        (doc >?> element("div[id=title-container]") >?> element("span[class=rank]")).flatten.map(_.text)
      ).left.map(single_error_enricher("adj_margin_rank"))

      season_stats_table_or_error =
        get_stats(doc, "function tableStart", "function")
          .left.map(single_error_enricher("season_stats"))

      conf_stats_table_or_error =
        get_stats(doc, "if (checked)", "function")
          .left.map(single_error_enricher("conf_stats"))

      table_tuple <- (season_stats_table_or_error, conf_stats_table_or_error).parMapN((_, _))
      (season_stats_table, conf_stats_table) = table_tuple

      season_stats_or_error =
        parse_season_stats(season_stats_table)
          .left.map(multi_error_enricher("total_stats"))

      metrics_tuple <- (adj_margin_rank_or_error, season_stats_or_error).parMapN((_, _))
      (adj_margin_rank, season_stats) = metrics_tuple

      adj_margin_value = season_stats.adj_off.value - season_stats.adj_def.value

    } yield season_stats.copy(
      adj_margin = Metric(adj_margin_value, adj_margin_rank)
    )
  }

  /** Gets the metric and ranking off the HTML fragment from the script */
  protected def parse_season_stats(in: Map[String, Either[ParseError, Document]]):
    Either[List[ParseError], TeamSeasonStats] =
  {
    val root: Either[List[ParseError], Unit] = Right(())
    def multi_error_enricher(field: String) = ParseUtils.enrich_sub_errors("", field) _
    def parse_stats_map(in: Option[Either[ParseError, Document]]): Either[List[ParseError], Metric] = {
      in.map(_.map(Some(_))).getOrElse(Right(None)) //(swap the option and either)
        .left.map(List(_))
        .flatMap(get_metric(_))
    }
    for {
      _ <- root

      //TODO: use shapeless to convert this to an HList

      adj_off_or_error = parse_stats_map(in.get("td#OE"))
        .left.map(multi_error_enricher("adj_off"))

      adj_def_or_error = parse_stats_map(in.get("td#DE"))
        .left.map(multi_error_enricher("adj_def"))

      def_to_or_error = parse_stats_map(in.get("td#DTOPct"))
        .left.map(multi_error_enricher("def_to"))

      all_stats <- (
        adj_off_or_error, adj_def_or_error,
        def_to_or_error
      ).parMapN((_, _, _))
      (adj_off, adj_def, def_to) = all_stats

    } yield TeamSeasonStats(adj_margin = Metric(0.0, 0), adj_off, adj_def, def_to)
  }

  /** All the stats are created by a JS function that inserts different HTML
      depending on whether the "conf" vs "season" checkbox is checked
      This function locates the relevant portion of the script section in the HTML
      and parses all the metrics into a large map
  */
  protected def parse_stats_table(doc: Document, start_token: String, end_token: String):
    Either[ParseError, String] =
  {
    def get_substring(script: String): Either[ParseError, String] = for {
      start <- ParseUtils.parse_string_offset(script, start_token, "start_token")
      sub_str1 = script.substring(start + start_token.length)
      end <- ParseUtils.parse_string_offset(sub_str1, end_token, "end_token")
    } yield sub_str1.substring(0, end)

    val scripts = (doc >> elementList("script")).map { script_el =>
      get_substring(script_el.innerHtml)
    }
    scripts.collect {
      case Right(script_fn) => script_fn
    }.headOption match {
      case Some(script_fn) =>
        Right(script_fn)

      case None =>
        Left(ParseUtils.build_sub_error("")(
          s"Failed to locate script containing token [$start_token] then [$end_token] in list [$scripts]"
        ))
    }
  }

  /** Parses a script function containing all the efficiency values into
      a map of the HTML, example:
      $("td#ARate").html("<a href=\"teamstats.php?s=RankARate\">54.9</a> <span class=\"seed\">6</span>");
  */
  protected def parse_script_function(in: String): Map[String, Either[ParseError, Document]] = {
    val browser = JsoupBrowser()
    val HtmlRegex = """^[^$]*[$][(]"([^"]+)"[)][.]html[(](".*")[)];[^;]*""".r
    in.lines.collect {
      case HtmlRegex(element_id, html) =>
        val map_value = parse(html).map(_.asString) match {
          case Right(Some(html_str)) =>
            ParseUtils.build_sub_request[Document](element_id)(browser.parseString(html_str))
          case e @ _ =>
            Left(ParseUtils.build_sub_error(element_id)(
              s"Failed to create HTML for [$element_id] - input was [$html], result was [$e]"
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

  /** Pulls out the rank and score from the format consistently used in HTML fragments */
  protected def get_metric(sub_doc: Option[Document]): Either[List[ParseError], Metric] =
  {
    def add_context(context: String)(error: ParseError): ParseError = {
      error.copy(messages = error.messages.map(_ + s" context=[$context]"))
    }
    val root: Either[List[ParseError], Unit] = Right(())
    sub_doc match {
      case Some(html) => for {
        _ <- root

        score_or_error =
          ParseUtils.parse_score((html >?> element("a")).map(_.text))
          .left.map(add_context(html.toHtml))
          .left.map(List(_))
        rank_or_error =
          ParseUtils.parse_rank((html >?> element("span[class=seed]")).map(_.text))
          .left.map(add_context(html.toHtml))
          .left.map(List(_))

        score_rank <- (score_or_error, rank_or_error).parMapN((_, _))
        (score, rank) = score_rank

      } yield Metric(score, rank)

      case None =>
        Left(List(ParseUtils.build_sub_error("value")(
          s"Failed to locate the field"
        )))
    }
  }

}
object TeamParser extends TeamParser
