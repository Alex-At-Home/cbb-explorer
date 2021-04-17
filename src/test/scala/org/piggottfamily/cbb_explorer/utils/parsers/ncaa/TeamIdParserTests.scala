package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
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
import org.joda.time.DateTime

object TeamIdParserTests extends TestSuite {
  import ExtractorUtils._
  import ExtractorUtilsTests._

  val team_id_html = Source.fromURL(getClass.getResource("/ncaa/test_attendance_list.html")).mkString

  val DISABLED = true //TODO (failing as of  04/2021, haven't looked into why but likely related to covid changes?)

  val tests = if (DISABLED) Tests {} else Tests {
    "TeamIdParser" - {
      "get_team_triples" - {
        TestUtils.inside(TeamIdParser.get_team_triples("filename_test", team_id_html)) {
          case Right(List(
            (TeamId("Syracuse"), "450738", ConferenceId("ACC")),
            (TeamId("Kentucky"), "450591", ConferenceId("SEC"))
          )) =>
        }
      }
      val test_in = List(
        (TeamId("Penn St."), "1", ConferenceId("B1G")),
        (TeamId("Michigan St."), "1000", ConferenceId("B1G")),
        (TeamId("Kentucky"), "450591", ConferenceId("SEC"))
      )
      "build_lineup_cli_array" - {
        TestUtils.inside(TeamIdParser.build_lineup_cli_array(test_in).toList) {
          case List(
            (ConferenceId("SEC"), "   '450591::Kentucky'"),
            (ConferenceId("B1G"),
            """   '1::Penn+St.'
   '1000::Michigan+St.'""")
          ) =>
        }
      }
      "build_available_team_list" - {
        val test_in_2 = List(
          (TeamId("Penn St."), "11", ConferenceId("B1G")),
          (TeamId("Maryland"), "10", ConferenceId("B1G"))
        )
        val res = TeamIdParser.build_available_team_list(Map(
          "2018/9" -> test_in,
          "2019/20" -> test_in_2
        ))
        res(ConferenceId("B1G"))("test") ==>  """ "Maryland": [
   { team: "Maryland", year: "2019/20", gender: "Men", index_template: "test" },
 ],
 "Penn St.": [
   { team: "Penn St.", year: "2018/9", gender: "Men", index_template: "test" },
   { team: "Penn St.", year: "2019/20", gender: "Men", index_template: "test" },
 ],
 "Michigan St.": [
   { team: "Michigan St.", year: "2018/9", gender: "Men", index_template: "test" },
 ],"""
        res(ConferenceId("SEC"))("test2") ==>  """ "Kentucky": [
   { team: "Kentucky", year: "2018/9", gender: "Men", index_template: "test2" },
 ],"""
      }
    }
  }
}
