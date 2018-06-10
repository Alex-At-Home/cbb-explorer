package org.piggottfamily.cbb_explorer.utils.parsers.kenpom

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.kenpom._
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

  import ExtractorUtils._

  protected val `kenpom.parse_team` = "kenpom.parse_team"
  protected val `kenpom.parse_team.parse_games` = "kenpom.parse_team.parse_games"
  protected val `kenpom.parse_team.parse_players` = "kenpom.parse_team.parse_players"

  /** Injected dependency of parsing game info */
  protected def game_parser = GameParser

  /** This should be the only object that is edited as stats are added to TeamSeasonStats */
  protected object builders {

    // Useful documentation for JSoup:
    // https://jsoup.org/cookbook/extracting-data/selector-syntax

    // Models and extraction instruction subsets for the case classes

    val team_model = LabelledGeneric[TeamSeason]
    val team = {
      var f: TeamSeason = null // (just used to infer type in "nameOf")
      // Note this list has to be in order of parameters in TeamSeasonStats
      // (compile error otherwise)
      Symbol(nameOf(f.coach)) ->> HtmlExtractor(
        e => e >?> element("span[class=coach] a"),
        e => Right(CoachId(e.text))
      ) ::
      Symbol(nameOf(f.conf)) ->> HtmlExtractor(
        e => e >?> element("span[class=otherinfo] a"),
        e => Right(ConferenceId(e.text))
      ) ::
      Symbol(nameOf(f.ncaa_seed)) ->> HtmlExtractor(
        e => e >?> element("td:matches(.*NCAA Tournament.*[0-9]+ seed.*)"),
        e => parse_ncaa_seed(e.text),
        fallback = Some(None) // ie do have a fallback, T=Option[Seed] so the fallback is none
      ) ::
      HNil
    }


    val season_stats_model = LabelledGeneric[TeamSeasonStats]
    val season_stats_off_def_model = LabelledGeneric[TeamSeasonStats.OffenseDefenseStats]
    val season_stats_sos_model = LabelledGeneric[TeamSeasonStats.StrengthOfSchedule]
    val season_stats_personnel_model = LabelledGeneric[TeamSeasonStats.Personnel]
    private def season_stats_off_def(off_not_def: Boolean, year: Year) = {
      val prefix = if (off_not_def) "" else "D" //(most parameters differ like this)
      val suffix = if (off_not_def) "O" else "D" //(odd case)
      var f: TeamSeasonStats.OffenseDefenseStats = null // (just used to infer type in "nameOf")
      // Note this list has to be in order of parameters in TeamSeasonStats
      // (compile error otherwise)
      Symbol(nameOf(f.avg_poss_len)) ->> OptionalScriptMetricExtractor(
        s"td#APL${suffix}",
        l => if (year.value < 2010) Nil else l, //(data not available pre-2010)
      ) ::
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
      def common_extractor(title: String, e: Element, skip: Int = 0): Option[Element] = {
        (e >?> elementList(s"tr:contains($title) td"))
          .getOrElse(Nil).drop(1 + skip).headOption
      }
      Symbol(nameOf(f.off)) ->> HtmlMetricExtractor(
        e => common_extractor("components:", e)
      ) ::
      Symbol(nameOf(f._def)) ->> HtmlMetricExtractor(
        e => common_extractor("components:", e, skip = 1)
      ) ::
      Symbol(nameOf(f.total)) ->> HtmlMetricExtractor(
        e => common_extractor("overall:", e)
      ) ::
      Symbol(nameOf(f.non_conf)) ->> HtmlMetricExtractor(
        e => common_extractor("non-conference:", e)
      ) ::
      HNil
    }
    def season_stats_personnel(year: Year) = {
      var f: TeamSeasonStats.Personnel = null // (just used to infer type in "nameOf")
      def common_extractor(title: String, e: Element): Option[Element] = {
        (e >?> elementList(s"tr:contains($title) td"))
          .getOrElse(Nil).drop(1).headOption
      }
      Symbol(nameOf(f.bench_mins_pct)) ->> HtmlMetricExtractor(
        e => common_extractor("bench minutes:", e)
      ) ::
      Symbol(nameOf(f.experience_yrs)) ->> HtmlMetricExtractor(
        e => common_extractor("experience:", e)
      ) ::
      Symbol(nameOf(f.continuity_pct)) ->> OptionalHtmlMetricExtractor(
        e => common_extractor("minutes continuity", e),
        l => if (year.value < 2008) Nil else l.filter { err => //(data not available pre-2008)
          err.messages.filterNot(_.contains("N/A")).nonEmpty //(if N/A then just ignore)
        },
      ) ::
      Symbol(nameOf(f.avg_height_inches)) ->> HtmlMetricExtractor(
        e => common_extractor("average height:", e)
      ) ::
      HNil
    }
    def season_stats(year: Year) = {
      var f: TeamSeasonStats = null // (just used to infer type in "nameOf")
      // Note this list has to be in order of parameters in TeamSeasonStats
      // (compile error otherwise)
      Symbol(nameOf(f.adj_margin)) ->> HtmlExtractor(
        e => e >?> element("div[id=title-container] span[class=rank]"),
        e => ParseUtils.parse_rank(Some(e.text)).map(Metric(Metric.no_value, _))
      ) ::
      Symbol(nameOf(f.adj_off)) ->> ScriptMetricExtractor("td#OE") ::
      Symbol(nameOf(f.adj_def)) ->> ScriptMetricExtractor("td#DE") ::
      Symbol(nameOf(f.adj_tempo)) ->> ScriptMetricExtractor("td#Tempo") ::
      Symbol(nameOf(f.sos)) ->> ChildExtractor(
        season_stats_sos, season_stats_sos_model
      ) ::
      Symbol(nameOf(f.personnel)) ->> OptionalChildExtractor(
        season_stats_personnel(year),
        l => if (year.value < 2007) Nil else l, //(data not available pre-2007)
        season_stats_personnel_model
      ) ::
      //(can't do off and _def as part of HList because the nesting breaks the compiler)
      HNil
    }
    def season_stats_off(year: Year) = Symbol(nameOf[TeamSeasonStats](_.off)) ->>
      ChildExtractor(
        season_stats_off_def(off_not_def = true, year),
        season_stats_off_def_model
      ) :: HNil
    def season_stats_def(year: Year) = Symbol(nameOf[TeamSeasonStats](_._def)) ->>
      ChildExtractor(
        season_stats_off_def(off_not_def = false, year),
        season_stats_off_def_model
      ) :: HNil
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
    val game_completer = ParseUtils.enrich_sub_errors(`kenpom.parse_team.parse_games`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      // Get team name and year
      team_season <- parse_filename(filename, default_year).left.map(multi_error_completer)

      // Misc fields (Eg coach, conference)
      other_fields_or_errors = ParseUtils.sequence_kv_results {
        object doc_extractor extends HtmlExtractorMapper {
          override val root = doc.root
        }
        builders.team map doc_extractor
      }.left.map(multi_error_completer)

      // Get basic ranking and metrics
      metrics_or_errors = parse_metrics(doc, team_season.year).left.map(multi_error_completer)

      // TODO Get players

      various_fields <- (
        other_fields_or_errors, metrics_or_errors
      ).parMapN((_, _))
      (other_fields, metrics) = various_fields //SI-5589

      // (Games depends on metrics + year)
      games <- game_parser.parse_games(
        doc, team_season.year, metrics.adj_margin.rank
      ).left.map(game_completer)

    } yield ParseResponse({
      var f: TeamSeason = null // (just used to infer type in "nameOf")
      builders.team_model.from(
        Symbol(nameOf(f.team_season)) ->> team_season ::
        Symbol(nameOf(f.stats)) ->> metrics ::
        Symbol(nameOf(f.games)) ->> games ::
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

  /** Parses out the season and conference metrics from various spots in the HTML
      and (ugh) script functions
  */
  protected def parse_metrics(doc: Document, current_year: Year): Either[List[ParseError], TeamSeasonStats] = {

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
      (season_stats_table, conf_stats_table) = table_tuple //SI-5589

      // Season stats is split into top level, offense, defense

      metrics_tuple <- {
        object extractor extends ChildExtractorMapper {
          override val root = doc.root
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
            builders.season_stats(current_year) map extractor
          ) ::
          // (For performance reasons, we write these 2 separately ... the compiler
          //  can't handle nesting these large HLists)
          ParseUtils.sequence_kv_results(
            builders.season_stats_off(current_year) map extractor
          ) ::
          ParseUtils.sequence_kv_results(
            builders.season_stats_def(current_year) map extractor
          ) ::
          HNil
        }.map(enricher).tupled.parMapN((_, _, _)) //SI-5589
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

  /** Extracts the NCAA seed from the table label that contains that info */
  protected def parse_ncaa_seed(seed_text: String): Either[ParseError, Option[Seed]] = {
    val seed_extractor = "NCAA Tournament - ([0-9]+) seed".r
    seed_text match {
      case seed_extractor(seed) if seed != null =>
        Right(Some(Seed(seed.toInt)))
      case _ =>
        Left(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Unexpected seed format: [$seed_text]"
        ))
    }
  }

  //TODO: parse_players

}
object TeamParser extends TeamParser
