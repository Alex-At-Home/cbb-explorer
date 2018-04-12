package org.piggottfamily.cbb_explorer.utils.parsers.kenpom

import utest._
import shapeless._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.TestUtils
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._
import scala.io.Source
import com.github.dwickern.macros.NameOf._
import shapeless._
import ops.hlist._
import shapeless.labelled._
import record._
import ops.record._
import syntax.singleton._

object TeamParserTests extends TestSuite with TeamParser {
  def get_doc(html: String): Document = {
    val browser = JsoupBrowser()
    browser.parseString(html)
  }
  def with_doc(html: String)(test: Document => Unit): Unit = {
    val doc = get_doc(html)
    test(doc)
  }

  val tests = Tests {
    "TeamParser" - {
      "parse_filename" - {
        TestUtils.inside(parse_filename("team00002010_rabbit.html", Year(1066))) {
          case Right(TeamSeasonId(TeamId("rabbit"), Year(2010))) =>
        }
        TestUtils.inside(parse_filename("team0000_rabbit.html", Year(1066))) {
          case Right(TeamSeasonId(TeamId("rabbit"), Year(1066))) =>
        }
        TestUtils.inside(parse_filename("complete_failure", Year(1066))) {
          case Left(List(ParseError("", "", _))) =>
        }
        TestUtils.inside(parse_filename("teamaaaa_.html", Year(1066))) {
          case Left(List(
            ParseError("", "[team]", _)
          )) =>
        }
      }
      "parse_html" - {
        val extractor = builders.HtmlExtractor(
          d => (d >?> element("span[class=coach]") >?> element("a")).flatten,
          CoachId(_)
        )
        with_doc(""" <span class="coach">Head coach: <a href="test">CoachName</a></span> """) { doc =>
          TestUtils.inside(parse_html(doc, extractor, "coach")) {
            case Right(CoachId("CoachName")) =>
          }
        }
        with_doc(""" <span class="team"><div><a>CoachName</a></div></span> """) { doc =>
          TestUtils.inside(parse_html(doc, extractor, "coach")) {
            case Left(ParseError("", "[coach]", _)) =>
          }
        }
      }
      "get_metric" - {
        TestUtils.inside(get_metric(None)) {
          case Left(List(ParseError("", "[value]", _))) =>
        }
        val fragment1 = """ <a href="teamstats.php?s=RankARate">100.0</a> """
        val fragment2 = """ <span class="seed">10</span> """

        List(fragment1, fragment2, fragment1 + fragment2, "").foreach { html =>
          with_doc(html) { doc =>
            TestUtils.inside(get_metric(Some(doc))) {
              case Left(List(ParseError("", "[value]", _), ParseError("", "[rank]", _))) if html.isEmpty  =>
              case Left(List(ParseError("", "[value]", _))) if !html.contains(fragment1) =>
              case Left(List(ParseError("", "[rank]", _))) if !html.contains(fragment2) =>
              case Right(Metric(100.0, 10)) =>
            }
          }
        }
      }
      "[script_functions]" - {
        val html_str = """
          $("td#ARate").html("<div class=\"result\">A1</div> <div class=\"test\">A2</div>");
          $("td#BRate").html("<div class=\"result\">B1</div> <div class=\"test\">B2</div>");
          IGNORE_THIS_LINE.html("<div class=\"result\">C1</div> <div class=\"test\">C2</div>");
          $("td#DRate").html(" " ");
        """

        "parse_stats_table" - {
          with_doc(s"""
            <script>END START</script>
            <script>START</script>
            <script>START${html_str}END</script>
          """) { doc =>
            TestUtils.inside(parse_stats_table(doc, "START", "END")) {
              case Right(`html_str`) =>
            }
            TestUtils.inside(parse_stats_table(doc, "NO_START", "END")) {
              case Left(ParseError("", "", _)) =>
            }
          }
        }
        "parse_script_function" - {
          TestUtils.inside(parse_script_function(html_str).toList) {
            case List(
                ("td#ARate", Right(d_a)),
                ("td#BRate", Right(d_b)),
                ("td#DRate", Left(ParseError("", "[td#DRate]", _))),
              ) =>
                (d_a >?> element("div[class=result]")).map(_.text) ==> Some("A1")
                (d_b >?> element("div[class=result]")).map(_.text) ==> Some("B1")
          }
        }
      }
      "parse_season_stats" - {
        object generate_doc extends Poly1 {
          implicit def doc = at[String](s => get_doc(s))
        }
        ("<a href=\"teamstats.php?s=RankARate\">11.1</a> <span class=\"seed\">1</span>"
          :: "<a href=\"teamstats.php?s=RankARate\">22.2</a> <span class=\"seed\">2</span>"
          :: "<bad></bad>"
          :: HNil
        ).map(generate_doc).tupled match { case (off_doc, def_doc, bad_doc) =>

          // Each stat field should generate 2 errors: one for its value and one for its rank
          val expected_error_list = {
            object generate_error_fields extends Poly1 {
              implicit def field[K <: Symbol](implicit key: Witness.Aux[K]) =
                at[FieldType[K, builders.ScriptMetricExtractor]](_ => {
                  val keyname = key.value.name
                  List(s"[$keyname][value]", s"[$keyname][rank]")
                })
            }
            (builders.season_stats map generate_error_fields).toList.flatten.map(
              field => ParseError("", field, Nil)
            )
          }

          // Assigns a given doc/metric based on whether a metric is offensive of defensive
          object generate_doc_metric extends Poly1 {
            implicit def doc_metric[K <: Symbol](implicit key: Witness.Aux[K]) =
              at[FieldType[K, builders.ScriptMetricExtractor]](kv => {
                val extractor: builders.ScriptMetricExtractor = kv
                field[K] {
                  extractor.path match {
                    case path if path.startsWith("td#D") =>
                      path -> (Metric(22.2, 2), def_doc)
                    case path =>
                      path -> (Metric(11.1, 1), off_doc)
                  }
                }
              })
          }
          // Generate a map filled with default valid values for each stat:
          val filled_map: Map[String, Either[ParseError, Document]] = {
            (builders.season_stats map generate_doc_metric).toList.toMap.mapValues {
              case (_, doc) => Right(doc)
            }
          }
          // The expected results - just pick out the metric from generate_doc_metric
          val expected_result = {
            object select_metric extends Poly1 {
              implicit def metric[K <: Symbol](implicit key: Witness.Aux[K]) =
                at[FieldType[K, (String, (Metric, Document))]](kv => {
                  //path -> (metric, doc) ... hence:
                  field[K](kv._2._1)
                })
            }
            builders.season_stats_model.from(
              Symbol(nameOf[TeamSeasonStats](_.adj_margin)) ->> Metric(0.0, 0) ::
              (builders.season_stats map generate_doc_metric map select_metric)
            )
          }

          // All good
          TestUtils.inside(parse_season_stats(filled_map)) {
            case Right(t) =>
              t ==> expected_result
          }
          // All bad
          TestUtils.inside(parse_season_stats(
            filled_map.mapValues(_ => Right(bad_doc))
          )) {
            case Left(l) =>
              l.map(_.copy(messages = Nil)) ==> expected_error_list
          }
          // Failed to parse doc
          TestUtils.inside(parse_season_stats(
            filled_map +
              ("td#DE" -> Left(ParseError("", "[doc_error]", List())))
          )) {
            case Left(List(ParseError("", "[adj_def][doc_error]", _))) =>
          }
          // Missing value
          TestUtils.inside(parse_season_stats(
            filled_map - "td#DE"
          )) {
            case Left(List(ParseError("", "[adj_def][value]", _))) =>
          }
          // Doc with missing value
          TestUtils.inside(parse_season_stats(
            filled_map +
              ("td#OE" -> Right(bad_doc)) -
              "td#DE"
          )) {
            case Left(List(
              ParseError("", "[adj_off][value]", _),
              ParseError("", "[adj_off][rank]", _),
              ParseError("", "[adj_def][value]", _)
            )) =>
          }
        }
      }
      "[file_tests]" - {
        val good_html = Source.fromURL(getClass.getResource("/teamb2512010_TestTeam___.html")).mkString

        val bad_stats_html_1 = good_html
          .replace("function tableStart", "function renamed_table_start")
          .replace("if (checked)", "if (renamed_checked)")

        val bad_stats_html_2 = good_html
          .replace("title-container", "title-container-2")
          .replace("td#OE", "td#renamed_OE")

        val bad_format_html = bad_stats_html_1
          .replace("coach", "rename_coach")

        val expected_team_stats =
          TeamSeasonStats(
            adj_margin = Metric(-1.0, 333),
            adj_off = Metric(101.1, 101), adj_def = Metric(102.1, 102),
            def_to = Metric(18.1, 108), def_stl = Metric(12.1, 122)
          )

        "parse_metrics" - {
          with_doc(good_html) { doc =>
            TestUtils.inside(parse_metrics(doc)) {
              case Right(`expected_team_stats`) =>
            }
          }
          with_doc(bad_stats_html_1) { doc =>
            TestUtils.inside(parse_metrics(doc)) {
              case Left(List(
                ParseError("", "[stats]", _),
                ParseError("", "[stats.conf_stats]", _)
              )) =>
            }
          }
          with_doc(bad_stats_html_2) { doc =>
            TestUtils.inside(parse_metrics(doc)) {
              case Left(List(
                ParseError("", "[adj_margin][rank]", _),
                ParseError("", "[stats][adj_off][value]", _)
              )) =>
            }
          }
        }
        "parse_team" - {
          val good_filename = "teamb2512010_TestTeam___.html"
          val good_filename_id = s"[$good_filename]"
          val bad_filename = "bad_filename"
          val bad_filename_id = s"[$bad_filename]"
          val root_prefix = "kenpom.parse_team"

          TestUtils.inside(parse_team(good_html, good_filename, Year(2000))) {
            case Right(ParseResponse(TeamSeason(
              TeamSeasonId(TeamId("TestTeam"), Year(2010)),
              `expected_team_stats`,
              Nil,
              players,
              CoachId("Coach Name"),
              ConferenceId("Atlantic Coast Conference")
            ), Nil)) if players.isEmpty =>
          }
          TestUtils.inside(parse_team("<>bad<ht>ml", good_filename, Year(2000))) {
            case Left(l @ List(
              ParseError(`root_prefix`, _, _),
              ParseError(`root_prefix`, _, _),
              ParseError(`root_prefix`, _, _),
              ParseError(`root_prefix`, _, _)
            )) =>
              l.map(_.id).zip(
                List("[coach]", "[conf]", "[stats]", "[stats.conf_stats]")
              ).foreach { case (id, sub_id) =>
                id ==> good_filename_id + sub_id
              }
          }
          TestUtils.inside(parse_team(good_html, bad_filename, Year(2000))) {
            case Left(List(
              ParseError(`root_prefix`, `bad_filename_id`, _)
            )) =>
          }
          TestUtils.inside(parse_team(bad_format_html, good_filename, Year(2000))) {
            case Left(l @ List(
              ParseError(`root_prefix`, _, _),
              ParseError(`root_prefix`, _, _),
              ParseError(`root_prefix`, _, _)
            )) =>
            l.map(_.id).zip(
              List("[coach]", "[stats]", "[stats.conf_stats]")
            ).foreach { case (id, sub_id) =>
              id ==> good_filename_id + sub_id
            }
          }
        }
      }
    }
  }
}
