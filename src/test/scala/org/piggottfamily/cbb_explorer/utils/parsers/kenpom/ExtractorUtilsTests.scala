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

  private def show_key[K <: Symbol, V](
    kv: FieldType[K, V])(implicit key: Witness.Aux[K]
  ): Symbol = key.value

  private def show_value[K, V](kv: FieldType[K, V]): V = {
    val v: V = kv
    v
  }

  private def get_doc(html: String): Document = {
    val browser = JsoupBrowser()
    browser.parseString(html)
  }
  private def with_doc(html: String)(test: Document => Unit): Unit = {
    val doc = get_doc(html)
    test(doc)
  }

  val tests = Tests {
    "ExtractorUtils" - {

        //TODO child extractor

      "[generic html extraction]" - {

        val extractor = HtmlExtractor(
          e => (e >?> element("span[class=coach]") >?> element("a")).flatten,
          e => Right(CoachId(e.text))
        )
        val good_doc = """ <span class="coach">Head coach: <a href="test">CoachName</a></span> """
        val bad_doc = """ <span class="team"><div><a>CoachName</a></div></span> """

        "HtmlExtractorMapper" - {
          with_doc(good_doc) { doc =>
            object mapper extends HtmlExtractorMapper {
              val root = doc.root
            }
            TestUtils.inside(
              'coach ->> extractor :: HNil map mapper
            ) {
              case good_result :: HNil =>
                show_value(good_result) ==> Right(CoachId("CoachName"))
                show_key(good_result) ==> 'coach
            }
          }
          with_doc(bad_doc) { doc =>
            object mapper extends HtmlExtractorMapper {
              val root = doc.root
            }
            TestUtils.inside(
              'coach ->> extractor :: HNil map mapper
            ) {
              case bad_result :: HNil =>
                TestUtils.inside(show_value(bad_result)) {
                  case Left(ParseError("", "[coach]", _)) =>
                }
            }
          }
        }
        "parse_html" - {
          with_doc(good_doc) { doc =>
            TestUtils.inside(parse_html(doc.root, extractor, "coach")) {
              case Right(CoachId("CoachName")) =>
            }
          }
          with_doc(bad_doc) { doc =>
            TestUtils.inside(parse_html(doc.root, extractor, "coach")) {
              case Left(ParseError("", "[coach]", _)) =>
            }
          }
        }
      }
      "[metric extractors]" - {
        val fragment1 = """ <a href="teamstats.php?s=RankARate">100.0</a> """
        val fragment2 = """ <span class="seed">10</span> """

        "HtmlMetricExtractor" - {
          val full_html = s"<body><p>$fragment1 $fragment2</p></body>"

          val good_extractor = HtmlMetricExtractor(
            e => e >?> element("p")
          )
          val bad_extractor = HtmlMetricExtractor(
            e => e >?> element("xxx")
          )
          with_doc(full_html) { doc =>
            object mapper extends HtmlMetricExtractorMapper {
              val root = doc.root
            }
            TestUtils.inside(
              'test1 ->> good_extractor ::
              'test2 ->> bad_extractor ::
              HNil map mapper)
            {
              case good_result :: bad_result :: HNil =>
                show_value(good_result) ==> Right(Metric(100.0, 10))
                show_key(good_result) ==> 'test1
                TestUtils.inside(show_value(bad_result)) {
                  case Left(List(ParseError(_, "[test2][value]", _))) =>
                }
            }
          }
        }
        "ScriptMetricExtractor" - {
          val partial_html = s"$fragment1 $fragment2"
          val test_map = Map(
            "test1" -> Left(ParseError("", "[test1_err]", List())),
            "test2" -> Right(get_doc(partial_html)),
            "test3" -> Right(get_doc(fragment1))
          )
          val extractor1 = ScriptMetricExtractor("test1")
          val extractor2 = ScriptMetricExtractor("test2")
          val extractor3 = ScriptMetricExtractor("test3")
          val extractor4 = ScriptMetricExtractor("test4")
          object mapper extends ScriptMetricExtractorMapper {
            val map = test_map
          }
          TestUtils.inside(
            'f_test1 ->> extractor1 ::
            'f_test2 ->> extractor2 ::
            'f_test3 ->> extractor3 ::
            'f_test4 ->> extractor4 ::
            HNil map mapper)
          {
            case bad_result1 :: good_result2 :: bad_result3 :: bad_result4 :: HNil =>
            TestUtils.inside(show_value(bad_result1)) {
              case Left(List(ParseError(_, "[f_test1][test1_err]", _))) =>
            }
              show_key(good_result2) ==> 'f_test2
              show_value(good_result2) ==> Right(Metric(100.0, 10))
              TestUtils.inside(show_value(bad_result3)) {
                case Left(List(ParseError(_, "[f_test3][rank]", _))) =>
              }
              TestUtils.inside(show_value(bad_result4)) {
                case Left(List(ParseError(_, "[f_test4][value]", _))) =>
              }
          }
        }
        "get_metric" - {
          TestUtils.inside(get_metric(None)) {
            case Left(List(ParseError("", "[value]", _))) =>
          }
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
}
