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

object TeamScheduleParserTests extends TestSuite with TeamScheduleParser {
  import ExtractorUtils._
  import ExtractorUtilsTests._

  val schedule_html =
    Source.fromURL(getClass.getResource("/ncaa/test_schedule.html")).mkString

  val tests = Tests {
    "TeamScheduleParser" - {
      "get_neutral_games" - {
        TestUtils.inside(
          get_neutral_games(
            "test_schedule.html",
            schedule_html,
            format_version = 0
          )
        ) {
          case Right(
                (TeamId("TEAM_NAME"), date_set)
              ) =>
            date_set.toList ==> List(
              "12/08/2018",
              "01/26/2019",
              "03/23/2019",
              "03/21/2019",
              "03/14/2019"
            )
        }
      }
    }
  }
}
