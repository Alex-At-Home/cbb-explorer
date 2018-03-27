package org.piggottfamily.cbb_explorer.utils.parsers.kenpom

import utest._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.TestUtils
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._

object TeamParserTests extends TestSuite with TeamParser {
  def withDoc(html: String)(test: Document => Unit): Unit = {
    val browser = JsoupBrowser()
    val doc = browser.parseString(html)
    test(doc)
  }

  val tests = Tests {
    "TeamParser" - {
      "parse_filename" - {
        TestUtils.inside(parse_filename("team00002010_rabbit.html")) {
          case Right(TeamSeasonId(TeamId("rabbit"), Year(2010))) =>
        }
        TestUtils.inside(parse_filename("complete_failure")) {
          case Left(List(ParseError("", "", _))) =>
        }
        TestUtils.inside(parse_filename("teamaaaa_.html")) {
          case Left(List(
            ParseError("", "[year]", _),
            ParseError("", "[team_name]", _)
          )) =>
        }
      }
      "parse_coach" - {
        withDoc(""" <span class="coach"><div><a>CoachName</a></div></span> """) { doc =>
          TestUtils.inside(parse_coach(doc)) {
            case Right(CoachId("CoachName")) =>
          }
        }
        withDoc(""" <span class="team"><div><a>CoachName</a></div></span> """) { doc =>
          TestUtils.inside(parse_coach(doc)) {
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
          withDoc(html) { doc =>
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
          withDoc(s"""
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
    }
  }
}
