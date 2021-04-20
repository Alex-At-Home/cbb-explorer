package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.models.ncaa.LineupEvent._
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

object RosterParserTests extends TestSuite with RosterParser {
  import ExtractorUtils._
  import ExtractorUtilsTests._

  val lineup_html = Source.fromURL(getClass.getResource("/ncaa/sample_roster.html")).mkString
  val lineup_html_diacritic = lineup_html.replace("Akin, Daniel", "Akin, Daniél")
  val lineup_html_dup_check = lineup_html.replace("Davis, Brendan", "Akin, David")

  val tests = Tests {
    "RosterParser" - {
      TestUtils.inside(parse_roster("test.hmtl", lineup_html, TeamId("TeamA")).map(_.take(5))) {
        case Right(List(
          RosterEntry(PlayerCodeId("DaAkin",PlayerId("Akin, Daniel")),"30","6-9","Sr", 16),
          RosterEntry(PlayerCodeId("JaBoonyasith",PlayerId("Boonyasith, Jacob")),"41","6-3","Jr", 14),
          RosterEntry(PlayerCodeId("BrDavis",PlayerId("Davis, Brendan")),"24","6-0","Fr", 3),
          RosterEntry(PlayerCodeId("RjEytle-rock",PlayerId("Eytle-Rock, R.J.")),"11","6-3","Jr", 20),
          RosterEntry(PlayerCodeId("SaGrace",PlayerId("Grace, Sam")),"15","5-10","So", 2)
        )) =>
      }
      TestUtils.inside(parse_roster("test.hmtl", lineup_html_dup_check, TeamId("TeamB")).map(_.take(5))) {
        case Left(_) =>
      }
    }
  }
}
