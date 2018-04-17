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

object ExtractorUtilsTests extends TestSuite {
  import ExtractorUtils._
  
  def get_doc(html: String): Document = {
    val browser = JsoupBrowser()
    browser.parseString(html)
  }
  def with_doc(html: String)(test: Document => Unit): Unit = {
    val doc = get_doc(html)
    test(doc)
  }

  val tests = Tests {
    "ExtractorUtils" - {

        //TODO each of the extractors
        "parse_html" - {
          val extractor = HtmlExtractor(
            d => (d >?> element("span[class=coach]") >?> element("a")).flatten,
            t => Right(CoachId(t))
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
              TestUtils.inside(get_metric(Some(doc.body))) {
                case Left(List(ParseError("", "[value]", _), ParseError("", "[rank]", _))) if html.isEmpty  =>
                case Left(List(ParseError("", "[value]", _))) if !html.contains(fragment1) =>
                case Left(List(ParseError("", "[rank]", _))) if !html.contains(fragment2) =>
                case Right(Metric(100.0, 10)) =>
              }
            }
          }
        }
    }
  }
}
