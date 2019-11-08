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

  val tests = Tests {
    "TeamIdParser" - {
      TestUtils.inside(TeamIdParser.get_team_triples("filename_test", team_id_html)) {
        case Right(List(
          (TeamId("Syracuse"), "450738", ConferenceId("ACC")),
          (TeamId("Kentucky"), "450591", ConferenceId("SEC"))
        )) =>
      }
      val test_in = List(
        (TeamId("Penn St."), "1", ConferenceId("B1G")),
        (TeamId("Michigan St."), "1000", ConferenceId("B1G")),
        (TeamId("Kentucky"), "450591", ConferenceId("SEC"))
      )
      TestUtils.inside(TeamIdParser.build_lineup_cli_array(test_in).toList) {
        case List(
          (ConferenceId("SEC"), "450591::Kentucky"),
          (ConferenceId("B1G"),
          """1::Penn+St.
1000::Michigan+St.""")
        ) =>
      }
    }
  }
}
