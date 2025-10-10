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

  val lineup_html =
    Source.fromURL(getClass.getResource("/ncaa/sample_roster.html")).mkString
  val lineup_html_diacritic =
    lineup_html.replace("Akin, Daniel", "Akin, Daniél")
  val lineup_html_dup_check =
    lineup_html.replace("Davis, Brendan", "Akin, David")

  val tests = Tests {
    "RosterParser" - {
      TestUtils.inside(
        parse_roster(
          "test.hmtl",
          lineup_html,
          TeamId("TeamA"),
          version_format = 0
        ).map(_.take(6))
      ) {
        case Right(
              List(
                RosterEntry(
                  PlayerCodeId(
                    "RjEytle-rock",
                    PlayerId("Eytle-Rock, R.J."),
                    Some("2084828")
                  ),
                  "11",
                  "G",
                  "6-3",
                  Some(75),
                  "Jr",
                  20,
                  None,
                  None
                ),
                RosterEntry(
                  PlayerCodeId(
                    "DiSpasojevic",
                    PlayerId("Spasojevic, Dimitrije"),
                    Some("1958411")
                  ),
                  "32",
                  "F",
                  "6-8",
                  Some(80),
                  "Sr",
                  20,
                  None,
                  None
                ),
                RosterEntry(
                  PlayerCodeId(
                    "DaRogers",
                    PlayerId("Rogers, Darnell"),
                    Some("2265903")
                  ),
                  "2",
                  "G",
                  "5-2",
                  Some(62),
                  "Sr",
                  20,
                  None,
                  None
                ),
                RosterEntry(
                  PlayerCodeId(
                    "KeKennedy",
                    PlayerId("Kennedy, Keondre"),
                    Some("2265883")
                  ),
                  "0",
                  "G",
                  "6-6",
                  Some(78),
                  "Jr",
                  20,
                  None,
                  None
                ),
                RosterEntry(
                  PlayerCodeId(
                    "BrHorvath",
                    PlayerId("Horvath, Brandon"),
                    Some("1954159")
                  ),
                  "12",
                  "F",
                  "6-10",
                  Some(82),
                  "Sr",
                  19,
                  None,
                  None
                ),
                RosterEntry(
                  PlayerCodeId(
                    "LjOwens",
                    PlayerId("Owens, L.J."),
                    Some("2081843")
                  ),
                  "1",
                  "G",
                  "6-3",
                  Some(75),
                  "Jr",
                  19,
                  None,
                  None
                )
                // (old roster entries prior to the sort by GP, in case I mess with sort again!)
                // RosterEntry(PlayerCodeId("DaAkin",PlayerId("Akin, Daniel")),"30","F", "6-9",Some(81),"Sr", 16),
                // RosterEntry(PlayerCodeId("JaBoonyasith",PlayerId("Boonyasith, Jacob")),"41","G","6-3",Some(75),"Jr", 14),
                // RosterEntry(PlayerCodeId("BrDavis",PlayerId("Davis, Brendan")),"24","G","6-0",Some(72),"Fr", 3),
                // RosterEntry(PlayerCodeId("SaGrace",PlayerId("Grace, Sam")),"15","G","5-10",Some(70),"So", 2)
              )
            ) =>
      }
      TestUtils.inside(
        parse_roster(
          "test.hmtl",
          lineup_html_dup_check,
          TeamId("TeamB"),
          version_format = 0
        ).map(_.take(5))
      ) { case Left(_) =>
      }
    }
  }
}
