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
  import ExtractorUtils._

  def get_doc(html: String): Document = {
    val browser = JsoupBrowser()
    browser.parseString(html)
  }
  def with_doc(html: String)(test: Document => Unit): Unit = {
    val doc = get_doc(html)
    test(doc)
  }

  //TODO add tests for the 2 extractors (and don't forget sequence_kv_results in ParseUtilsTests)

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
      "[file_tests]" - {
        val good_html = Source.fromURL(getClass.getResource("/teamb2512010_TestTeam___.html")).mkString

        val bad_stats_html_1 = good_html
          .replace("function tableStart", "function renamed_table_start")
          .replace("if (checked)", "if (renamed_checked)")

        val bad_stats_html_2 = good_html
          .replace("title-container", "title-container-2")
          .replace("td#OE", "td#renamed_OE")
          .replace("td#eFG", "td#renamed_eFG")
          .replace("td#DeFG", "td#renamed_DeFG")

        val bad_format_html = bad_stats_html_1
          .replace("coach", "rename_coach")

        // Sample lines:
        //$("td#OE").html("<a href=\"summary.php?s=RankAdjOE\">101.2</a> <span class=\"seed\">1</span>");
        //$("td#eFG").html("<a href=\"stats.php?s=RankeFG_Pct\">55.1</a> <span class=\"seed\">105</span>");
        //$("td#Tempo").html("<span style=\"background-color:#dfebfd; padding:3px 8px\"><a href=\"summary.php?s=RankAdjTempo\">92.1</a> <span class=\"seed\">120</span></span>");
        def get_stats_map =
          good_html.lines.flatMap { line =>
            val regex =
              """.*[$][(]"(td#[^"]+)"[)][.]html[(]".*<a[^>]+>([0-9.]+)<.*<span.*>([0-9]+)<.*""".r
            line match {
              case regex(path, value_str, rank_str) if value_str.endsWith(".1") =>
                // (if it doesn't end with .1 then it must be conf-only stats)
                List(path -> Metric(value_str.toDouble, rank_str.toInt))
              case _ =>
                Nil
            }
          }.toMap
        val mutable_expected_season_stats_map = collection.mutable.Map() ++ get_stats_map

        // Iterate over the fields and match up with the strings pulled
        val expected_team_stats = {
          object generate_expected_results extends Poly1 {
            implicit def get_field[K <: Symbol] =
              at[FieldType[K, ScriptMetricExtractor]](kv => {
                val extractor: ScriptMetricExtractor = kv
                field[K] {
                  //(throws if not present)
                  mutable_expected_season_stats_map.remove(extractor.path).getOrElse {
                    throw new Exception(
                      s"Missing [${extractor.path}], left = [$mutable_expected_season_stats_map]"
                    )
                  }
                }
              })
          }
          builders.season_stats_model.from({
            val t: TeamSeasonStats = null //(just for nameOf type inference)
            Symbol(nameOf(t.adj_margin)) ->> Metric(-1.0, 333) ::
            (builders.season_stats collect generate_expected_results) ::: //(this is list)
            Symbol(nameOf(t.sos)) ->> TeamSeasonStats.StrengthOfSchedule(
              off = Metric(100.1, 111), _def = Metric(100.2, 88),
              total = Metric(4.44, 77), non_conf = Metric(-1.66, 222),
            ) ::
            Symbol(nameOf(t.personnel)) ->> TeamSeasonStats.Personnel(
              bench_mins_pct = Metric(22.2, 199), experience_yrs = Metric(1.11, 198),
              continuity_pct = Metric(33.3, 333), avg_height_inches = Metric(77.0, 99)
            ) ::
            Symbol(nameOf(t.off)) ->> builders.season_stats_off_def_model.from(
              (builders.season_stats_off.head.fields map generate_expected_results)
            ) ::
            Symbol(nameOf(t._def)) ->> builders.season_stats_off_def_model.from(
              (builders.season_stats_def.head.fields map generate_expected_results)
            ) ::
            HNil
          })
        }

        "parse_metrics" - {
          with_doc(good_html) { doc =>
            TestUtils.inside(parse_metrics(doc)) {
              case Right(`expected_team_stats`) =>
                // Check we grabbed all the fields from the expected map
                mutable_expected_season_stats_map.toMap ==> Map.empty
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
                ParseError("", "[stats][adj_margin]", _),
                ParseError("", "[stats][adj_off][value]", _),
                ParseError("", "[stats][off][eff_fg][value]", _),
                ParseError("", "[stats][_def][eff_fg][value]", _)
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
