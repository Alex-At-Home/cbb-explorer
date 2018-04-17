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

    // Useful documentation for JSoup:
    // https://jsoup.org/cookbook/extracting-data/selector-syntax

    /** (TeamSeasonStats/parse_metrics) */
    case class ScriptMetricExtractor(path: String)
    /** (TeamSeason/parse_team) */
    case class HtmlExtractor[T](
      extract: Document => Option[Element],
      build: String => Either[ParseError, T]
    )
    /** (TeamSeasonStats/parse_metrics) */
    case class HtmlMetricExtractor(
      extract: Document => Option[Element]
    )
    /** (TeamSeasonStats/parse_metrics) */
    case class ChildExtractor[I <: HList, T](
      fields: I,
      model_builder: LabelledGeneric[T] //(unused except to provide the type T)
    )

    // Models and extraction instruction subsets for the case classes

    val team_model = LabelledGeneric[TeamSeason]
    val team = {
      var f: TeamSeason = null // (just used to infer type in "nameOf")
      // Note this list has to be in order of parameters in TeamSeasonStats
      // (compile error otherwise)
      Symbol(nameOf(f.coach)) ->> HtmlExtractor(
        d => (d >?> element("span[class=coach]") >?> element("a")).flatten,
        t => Right(CoachId(t))
      ) ::
      Symbol(nameOf(f.conf)) ->> HtmlExtractor(
        d => (d >?> element("span[class=otherinfo]") >?> element("a")).flatten,
        t => Right(ConferenceId(t))
      ) ::
      HNil
    }

    val season_stats_model = LabelledGeneric[TeamSeasonStats]
    val season_stats_off_def_model = LabelledGeneric[TeamSeasonStats.OffenseDefenseStats]
    val season_stats_sos_model = LabelledGeneric[TeamSeasonStats.StrengthOfSchedule]
    val season_stats_personnel_model = LabelledGeneric[TeamSeasonStats.Personnel]
    private def season_stats_off_def(off_not_def: Boolean) = {
      val prefix = if (off_not_def) "" else "D" //(most parameters differ like this)
      val suffix = if (off_not_def) "O" else "D" //(odd case)
      var f: TeamSeasonStats.OffenseDefenseStats = null // (just used to infer type in "nameOf")
      // Note this list has to be in order of parameters in TeamSeasonStats
      // (compile error otherwise)
      Symbol(nameOf(f.avg_poss_len)) ->> ScriptMetricExtractor(s"td#APL${suffix}") ::
      Symbol(nameOf(f.eff_fg)) ->> ScriptMetricExtractor(s"td#${prefix}eFG") ::
      Symbol(nameOf(f.to_pct)) ->> ScriptMetricExtractor(s"td#${prefix}TOPct") ::
      Symbol(nameOf(f.orb_pct)) ->> ScriptMetricExtractor(s"td#${prefix}ORPct") ::
      Symbol(nameOf(f.ft_rate)) ->> ScriptMetricExtractor(s"td#${prefix}FTR") ::
      Symbol(nameOf(f._3p_pct)) ->> ScriptMetricExtractor(s"td#${prefix}3Pct") ::
      Symbol(nameOf(f._2p_pct)) ->> ScriptMetricExtractor(s"td#${prefix}2Pct") ::
      Symbol(nameOf(f.ft_pct)) ->> ScriptMetricExtractor(s"td#${prefix}FTPct") ::
      Symbol(nameOf(f.blk_pct)) ->> ScriptMetricExtractor(s"td#${prefix}BlockPct") ::
      Symbol(nameOf(f.stl_pct)) ->> ScriptMetricExtractor(s"td#${prefix}StlRate") ::
      Symbol(nameOf(f._3pa_rate)) ->> ScriptMetricExtractor(s"td#${prefix}3PARate") ::
      Symbol(nameOf(f.afgm_rate)) ->> ScriptMetricExtractor(s"td#${prefix}ARate") ::
      Symbol(nameOf(f._3p_pt_dist)) ->> ScriptMetricExtractor(s"td#${prefix}PD3") ::
      Symbol(nameOf(f._2p_pt_dist)) ->> ScriptMetricExtractor(s"td#${prefix}PD2") ::
      Symbol(nameOf(f.ft_pt_dist)) ->> ScriptMetricExtractor(s"td#${prefix}PD1") ::
      HNil
      //(note this is too long to be nested in other HLists)
    }
    val season_stats_sos = {
      var f: TeamSeasonStats.StrengthOfSchedule = null // (just used to infer type in "nameOf")
      def common_extractor(title: String, d: Document): List[Element] = {
        (d >?> element(s"tr:contains($title)") >?> elementList("td"))
          .flatten.getOrElse(Nil).drop(1)
      }
      Symbol(nameOf(f.off)) ->> HtmlMetricExtractor(
        d => (common_extractor("components:", d).drop(0).headOption)
      ) ::
      Symbol(nameOf(f._def)) ->> HtmlMetricExtractor(
        d => (common_extractor("components:", d).drop(1).headOption)
      ) ::
      Symbol(nameOf(f.total)) ->> HtmlMetricExtractor(
        d => (common_extractor("overall:", d).drop(0).headOption)
      ) ::
      Symbol(nameOf(f.non_conf)) ->> HtmlMetricExtractor(
        d => (common_extractor("non-conference:", d).drop(0).headOption)
      ) ::
      HNil
    }
    val season_stats_personnel = {
      var f: TeamSeasonStats.Personnel = null // (just used to infer type in "nameOf")
      def common_extractor(title: String, d: Document): List[Element] = {
        (d >?> element(s"tr:contains($title)") >?> elementList("td"))
          .flatten.getOrElse(Nil).drop(1)
      }
      Symbol(nameOf(f.bench_mins_pct)) ->> HtmlMetricExtractor(
        d => (common_extractor("bench minutes:", d).drop(0).headOption)
      ) ::
      Symbol(nameOf(f.experience_yrs)) ->> HtmlMetricExtractor(
        d => (common_extractor("experience:", d).drop(0).headOption)
      ) ::
      Symbol(nameOf(f.continuity_pct)) ->> HtmlMetricExtractor(
        d => (common_extractor("minutes continuity", d).drop(0).headOption)
      ) ::
      Symbol(nameOf(f.avg_height_inches)) ->> HtmlMetricExtractor(
        d => (common_extractor("average height:", d).drop(0).headOption)
      ) ::
      HNil
    }
    val season_stats = {
      var f: TeamSeasonStats = null // (just used to infer type in "nameOf")
      // Note this list has to be in order of parameters in TeamSeasonStats
      // (compile error otherwise)
      Symbol(nameOf(f.adj_margin)) ->> HtmlExtractor(
        d => (d >?> element("div[id=title-container]") >?> element("span[class=rank]")).flatten,
        rank => ParseUtils.parse_rank(Some(rank)).map(Metric(Metric.no_value, _))
      ) ::
      Symbol(nameOf(f.adj_off)) ->> ScriptMetricExtractor("td#OE") ::
      Symbol(nameOf(f.adj_def)) ->> ScriptMetricExtractor("td#DE") ::
      Symbol(nameOf(f.adj_tempo)) ->> ScriptMetricExtractor("td#Tempo") ::
      Symbol(nameOf(f.sos)) ->> ChildExtractor(
        season_stats_sos, season_stats_sos_model
      ) ::
      Symbol(nameOf(f.personnel)) ->> ChildExtractor(
        season_stats_personnel, season_stats_personnel_model
      ) ::
      //(can't do off and _def as part of HList because the nesting breaks the compiler)
      HNil
    }
    val season_stats_off = Symbol(nameOf[TeamSeasonStats](_.off)) ->>
      ChildExtractor(
        season_stats_off_def(off_not_def = true),
        season_stats_off_def_model
      ) :: HNil
    val season_stats_def = Symbol(nameOf[TeamSeasonStats](_._def)) ->>
      ChildExtractor(
        season_stats_off_def(off_not_def = false),
        season_stats_off_def_model
      ) :: HNil
  }
  /** Enriches errors generated by child functions (single error) */
  private def single_error_enricher(field: String) = ParseUtils.enrich_sub_error(`parent_fills_in`, field) _
  /** Enriches errors generated by child functions (multiple errors) */
  private def multi_error_enricher(field: String) = ParseUtils.enrich_sub_errors(`parent_fills_in`, field) _

  /** HList mapper for converting semi-pre-parsed script metric fields */
  protected trait ScriptMetricExtractorMapper extends Poly1 {
    val map: Map[String, Either[ParseError, Document]]

    private def parse_stats_map(in: Option[Either[ParseError, Document]]): Either[List[ParseError], Metric] = {
      in.map(_.map(Some(_))).getOrElse(Right(None)) //(swap the option and either)
        .left.map(List(_))
        .flatMap(d => get_metric(d.map(_.body)))
    }
    implicit def script_metric_fields[K <: Symbol](implicit key: Witness.Aux[K]) =
      at[FieldType[K, builders.ScriptMetricExtractor]](kv => {
        val extractor: builders.ScriptMetricExtractor = kv
        field[K](
          parse_stats_map(map.get(extractor.path))
            .left.map(multi_error_enricher(key.value.name))
        ) //(returns FieldType[K, Either[List[ParseError], Metric])
      })
  }
  /** HList mapper for converting HTML fields */
  protected trait HtmlExtractorMapper extends Poly1 {
    val _doc: Document

    implicit def html_fields[K <: Symbol, T](implicit key: Witness.Aux[K]) =
      at[FieldType[K, builders.HtmlExtractor[T]]](kv => {
        val extractor: builders.HtmlExtractor[T] = kv
        field[K](
          parse_html(_doc, extractor, key.value.name)
        ) //(returns FieldType[K, Either[List[ParseError], T])
      })
  }
  /** HList mapper for converting HTML fields */
  protected trait HtmlMetricExtractorMapper extends Poly1 {
    val _doc: Document

    implicit def html_metric_fields[K <: Symbol, T](implicit key: Witness.Aux[K]) =
      at[FieldType[K, builders.HtmlMetricExtractor]](kv => {
        val extractor: builders.HtmlMetricExtractor = kv
        field[K](
          get_metric(extractor.extract(_doc))
            .left.map(multi_error_enricher(key.value.name))
        ) //(returns FieldType[K, Either[List[ParseError], Metric])
      })
  }

  /** Internal component for HList mapper that maps over nested HLists */
  protected trait NonRecursiveChildExtractorMapper
    extends Poly1
    with ScriptMetricExtractorMapper
    with HtmlExtractorMapper
    with HtmlMetricExtractorMapper

  /** HList mapper that maps over nested HLists */
  protected trait ChildExtractorMapper
    extends Poly1
    with NonRecursiveChildExtractorMapper
  {
    val myself = this
    object child_extractor extends NonRecursiveChildExtractorMapper {
      //(in theory should be able to use "this". but can't get it working in practice)
      override val map = myself.map
      override val _doc = myself._doc
    }

    implicit def children_fields
      [K <: Symbol, I <: HList, M1 <: HList, M2 <: HList, R <: HList, O]
      (implicit
        key: Witness.Aux[K],
        ogen: LabelledGeneric.Aux[O, R],
        child_mapper: Mapper.Aux[child_extractor.type, I, M1],
        // (need to bring in all the impplicits from ParseUtils.sequence_kv_results):
        right_only_mapper: Mapper.Aux[ParseUtils.right_only_kv.type, M1, R],
        left_or_filter_right_mapper: Mapper.Aux[ParseUtils.left_or_filter_right_kv.type, M1, M2],
        to_list: ToTraversable.Aux[M2, List, List[ParseError]]
      ) = at[FieldType[K, builders.ChildExtractor[I, O]]](kv => {
        val extractor: builders.ChildExtractor[I, O] = kv

        field[K](
          ParseUtils.sequence_kv_results(
            extractor.fields map child_extractor
          ).left.map(multi_error_enricher(key.value.name))
          .right.map(ogen.from(_))
        )//returns Either[List[ParseError], O]
      })
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
    val single_error_completer = ParseUtils.enrich_sub_error(`kenpom.parse_team`, filename) _
    val multi_error_completer = ParseUtils.enrich_sub_errors(`kenpom.parse_team`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      // Get team name and year
      team_season <- parse_filename(filename, default_year).left.map(multi_error_completer)

      // Misc fields (Eg coach, conference)
      other_fields_or_errors = ParseUtils.sequence_kv_results {
        object doc_extractor extends HtmlExtractorMapper {
          override val _doc = doc
        }
        builders.team map doc_extractor
      }.left.map(multi_error_completer)

      // Get basic ranking and metrics
      metrics_or_errors = parse_metrics(doc).left.map(multi_error_completer)

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
        extractor.build(result.text)
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

    def get_stats(doc: Document, start_token: String, end_token: String) = {
      parse_stats_table(doc, start_token, end_token).map(parse_script_function)
    }

    for {
      _ <- root

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

      // Season stats is split into top level, offense, defense

      metrics_tuple <- {
        object extractor extends ChildExtractorMapper {
          override val _doc = doc
          override val map = season_stats_table
        }
        object enricher extends Poly1 {
          implicit def enrich[O]
            = at[Either[List[ParseError], O]](v => v.left.map(
              multi_error_enricher(nameOf[TeamSeason](_.stats))
            ))
        }
        {
          ParseUtils.sequence_kv_results(
            builders.season_stats map extractor
          ) ::
          // (For performance reasons, we write these 2 separately ... the compiler
          //  can't handle nesting these large HLists)
          ParseUtils.sequence_kv_results(
            builders.season_stats_off map extractor
          ) ::
          ParseUtils.sequence_kv_results(
            builders.season_stats_def map extractor
          ) ::
          HNil
        }.map(enricher).tupled.parMapN((_, _, _))
      }

      (
        stats_misc, stats_off, stats_def
      ) = metrics_tuple

      result = builders.season_stats_model.from(
        stats_misc ::: //(stats_misc is a list of records)
        stats_off :::
        stats_def
      )
      // (final bit of adj margin)
      adj_margin_value = result.adj_off.value - result.adj_def.value

    } yield result.copy(
      adj_margin = result.adj_margin.copy(value = adj_margin_value)
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
      case HtmlRegex(element_id, html) =>        val f: TeamSeasonStats = null //(just for nameOf type inference)

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
  protected def get_metric(sub_doc: Option[Element]): Either[List[ParseError], Metric] =
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
          .left.map(add_context(html.outerHtml))
          .left.map(List(_))
        rank_or_error =
          ParseUtils.parse_rank((html >?> element("span[class=seed]")).map(_.text))
          .left.map(add_context(html.outerHtml))
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
