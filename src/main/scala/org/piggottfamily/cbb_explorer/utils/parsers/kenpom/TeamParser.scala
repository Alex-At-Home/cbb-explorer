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

/** Parses the team HTML */
trait TeamParser {

  protected val `kenpom.parse_team` = "kenpom.parse_team"
  protected val `kenpom.parse_team.parse_game` = "kenpom.parse_team.parse_game"
  protected val `parent_fills_in` = ""

  /** This should be the only object that is edited as stats are added to TeamSeasonStats */
  protected object builders {
    // Extractor types - note these are tightly coupled to specific case classes/parser fns
    // (see comments below)

    /** (TeamSeaonStats/parse_season_stats) */
    case class ScriptMetricExtractor(path: String)
    /** (TeamSeason/parse_team) */
    case class HtmlExtractor[T](
      extract: Document => Option[Element],
      build: String => T
    )

    // Models and extraction instruction subsets for the case classes

    val team_model = LabelledGeneric[TeamSeason]
    val team = {
      var f: TeamSeason = null // (just used to infer type in "nameOf")
      // Note this list has to be in order of parameters in TeamSeasonStats
      // (compile error otherwise)
      Symbol(nameOf(f.coach)) ->> HtmlExtractor(
        d => (d >?> element("span[class=coach]") >?> element("a")).flatten,
        CoachId(_)
      ) ::
      Symbol(nameOf(f.conf)) ->> HtmlExtractor(
        d => (d >?> element("span[class=otherinfo]") >?> element("a")).flatten,
        ConferenceId(_)
      ) ::
      HNil
    }

    val season_stats_model = LabelledGeneric[TeamSeasonStats]
    val season_stats = {
      var f: TeamSeasonStats = null // (just used to infer type in "nameOf")
      // Note this list has to be in order of parameters in TeamSeasonStats
      // (compile error otherwise)
      Symbol(nameOf(f.adj_off)) ->> ScriptMetricExtractor("td#OE") ::
      Symbol(nameOf(f.adj_def)) ->> ScriptMetricExtractor("td#DE") ::
      Symbol(nameOf(f.def_to)) ->> ScriptMetricExtractor("td#DTOPct") ::
      Symbol(nameOf(f.def_stl)) ->> ScriptMetricExtractor("td#DStlRate") ::
      HNil
    }
  }

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

    class extractor(doc: Document) extends Poly1 {
      implicit def fields[K <: Symbol, T](implicit key: Witness.Aux[K]) =
        at[FieldType[K, builders.HtmlExtractor[T]]](kv => {
          val extractor: builders.HtmlExtractor[T] = kv
          field[K](
            parse_html(doc, extractor, key.value.name)
              .left.map(single_error_enricher)
          ) //(returns FieldType[K, Either[List[ParseError], T])
        })
    }

    for {
      doc <- doc_request_builder(browser.parseString(in))

      // Get team name and year
      team_season <- parse_filename(filename, default_year).left.map(multi_error_enricher)

      // Misc fields (Eg coach, conference)
      other_fields_or_errors = ParseUtils.sequence_kv_results {
        object doc_extractor extends extractor(doc)
        builders.team map doc_extractor
      }

      // Get basic ranking and metrics
      metrics_or_errors = parse_metrics(doc).left.map(multi_error_enricher)

      // TODO Get games
      // TODO Get players

      various_fields <- (
        other_fields_or_errors, metrics_or_errors
      ).parMapN((_, _))
      (other_fields, metrics) = various_fields //SI-5589

    } yield ParseResponse({
      var f: TeamSeason = null // (just used to infer type in "nameOf")
      builders.team_model.from(
        Symbol(nameOf(f.team_season)) ->> team_season ::
        Symbol(nameOf(f.stats)) ->> metrics ::
        Symbol(nameOf(f.games)) ->> List[Game]() ::
        Symbol(nameOf(f.players)) ->> Map[PlayerId, PlayerSeasonSummaryStats]() ::
        other_fields
      )
    })
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
        val errors = List(
          nameOf[TeamSeasonId](_.year) -> year,
          nameOf[TeamSeasonId](_.team) -> team_name
        ).collect {
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
        Left(List(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Completely failed to parse [$filename] to extract year and team"
        )))
    }
  }

  /** Invokes and HTML extractor and AnyVal/case class buider and wraps with errors */
  protected def parse_html[T](doc: Document, extractor: builders.HtmlExtractor[T], fieldname: String)
  : Either[ParseError, T] =
  {
    extractor.extract(doc) match {
      case Some(result) =>
        Right(extractor.build(result.text))
      case None =>
        Left(ParseUtils.build_sub_error(fieldname)(
          s"Failed to parse HTML - couldn't extract [$fieldname]"
        ))
    }
  }

  /** Parses out the season and conference metrics from various spots in the HTML
      and (ugh) script functions
  */
  protected def parse_metrics(doc: Document): Either[List[ParseError], TeamSeasonStats] = {

    val root: Either[List[ParseError], Unit] = Right(())
    def single_error_enricher(field: String) = ParseUtils.enrich_sub_error(`parent_fills_in`, field) _
    def multi_error_enricher(field: String) = ParseUtils.enrich_sub_errors(`parent_fills_in`, field) _

    def get_stats(doc: Document, start_token: String, end_token: String) = {
      parse_stats_table(doc, start_token, end_token).map(parse_script_function)
    }

    for {
      _ <- root

      adj_margin_rank_or_error = ParseUtils.parse_rank(
        (doc >?> element("div[id=title-container]") >?> element("span[class=rank]")).flatten.map(_.text)
      ).left.map(single_error_enricher(nameOf[TeamSeasonStats](_.adj_margin)))

      season_stats_table_or_error =
        get_stats(doc, "function tableStart", "function")
          .left.map(single_error_enricher(nameOf[TeamSeason](_.stats)))

      conf_stats_table_or_error =
        get_stats(doc, "if (checked)", "function")
          .left.map(single_error_enricher{
            val `stats` = nameOf[TeamSeason](_.stats)
            val `conf_stats` = "conf_stats" //TODO: not yet used
            s"${`stats`}.${`conf_stats`}"
          })

      table_tuple <- (season_stats_table_or_error, conf_stats_table_or_error).parMapN((_, _))
      (season_stats_table, conf_stats_table) = table_tuple

      season_stats_or_error =
        parse_season_stats(season_stats_table)
          .left.map(multi_error_enricher(nameOf[TeamSeason](_.stats)))

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
    def multi_error_enricher(field: String) = ParseUtils.enrich_sub_errors(`parent_fills_in`, field) _
    def parse_stats_map(in: Option[Either[ParseError, Document]]): Either[List[ParseError], Metric] = {
      in.map(_.map(Some(_))).getOrElse(Right(None)) //(swap the option and either)
        .left.map(List(_))
        .flatMap(get_metric(_))
    }
    object extractor extends Poly1 {
      implicit def fields[K <: Symbol](implicit key: Witness.Aux[K]) =
        at[FieldType[K, builders.ScriptMetricExtractor]](kv => {
          val extractor: builders.ScriptMetricExtractor = kv
          field[K](
            parse_stats_map(in.get(extractor.path))
              .left.map(multi_error_enricher(key.value.name))
          ) //(returns FieldType[K, Either[List[ParseError], Metric])
        })
    }
    for {
      all_stats <- ParseUtils.sequence_kv_results(
        builders.season_stats.map(extractor)
      )
    } yield builders.season_stats_model.from( //(adj_margin is filled in later)
      Symbol(nameOf[TeamSeasonStats](_.adj_margin)) ->> Metric(0.0, 0) ::
      all_stats
    )
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
        Left(ParseUtils.build_sub_error(`parent_fills_in`)(
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
        val map_value = io.circe.parser.parse(html).map(_.asString) match {
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
        Left(List(ParseUtils.build_sub_error(nameOf[Metric](_.value))(
          s"Failed to locate the field"
        )))
    }
  }

}
object TeamParser extends TeamParser
