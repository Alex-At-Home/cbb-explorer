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

object BoxscoreParserTests extends TestSuite with BoxscoreParser {
  import ExtractorUtils._
  import ExtractorUtilsTests._

  val lineup_html = Source.fromURL(getClass.getResource("/ncaa/test_lineup.html")).mkString

  val tests = Tests {
    "BoxscoreParser" - {
      "get_lineup" - {
        { (1, 0.0) :: (2, 20.0) :: (3, 40.0) :: (4, 45.0) :: Nil }.foreach { case (period, mins) =>
          TestUtils.inside(get_lineup(lineup_html, s"test_p$period.html", Year(2015))) {
            case Right(LineupEvent(
              date, `mins`, `mins`, 0.0, 0,
              TeamSeasonId(TeamId("TeamA"), Year(2015)),
              TeamSeasonId(TeamId("TeamB"), Year(2015)),
              _, lineup, Nil, Nil, Nil, Nil, _, _
            )) =>
              date.toString ==> "2018-12-10T00:00:00.000-05:00" 
              lineup ==> {
                "S1rname, F1rstname TeamA" ::
                "S2rname, F2rstname TeamA" ::
                "S3rname, F3rstname TeamA" ::
                "S4rname, F4rstname TeamA" ::
                "S5rname, F5rstname TeamA" ::
                Nil
              }.map(build_player_code).sortBy(_.code)
          }
        }
      }
    }
  }
}
