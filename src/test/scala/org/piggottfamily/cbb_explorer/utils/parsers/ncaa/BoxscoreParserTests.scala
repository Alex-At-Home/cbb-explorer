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
          // Home
          TestUtils.inside(get_box_lineup(s"test_p$period.html", lineup_html, TeamId("TeamA"))) {
            case Right(LineupEvent(
              date, Game.LocationType.Away, `mins`, `mins`, 0.0, score,
              TeamSeasonId(TeamId("TeamA"), Year(2018)),
              TeamSeasonId(TeamId("TeamB"), Year(2018)),
              _, lineup, Nil, Nil, Nil, _, _
            )) =>
              date.toString ==> "2018-12-10T17:00:00.000-05:00"
              lineup ==> {
                "S1rname, F1rstname TeamA" ::
                "S2rname, F2rstname TeamA" ::
                "S3rname, F3rstname TeamA" ::
                "S4rname, F4rstname TeamA" ::
                "S5rname, F5rstname TeamA" ::
                "S6rname, F6rstname TeamA" ::
                "S7rname, F7rstname TeamA" ::
                "S8rname, F8rstname TeamA" ::
                "S9rname, F9rstname TeamA" ::
                Nil
              }.map(build_player_code).sortBy(_.code)

              score ==> LineupEvent.ScoreInfo(Game.Score(0,0),Game.Score(92,91),0,0)
          }
          // Away
          TestUtils.inside(
            get_box_lineup(s"test_p$period.html", lineup_html, TeamId("TeamB"), Set("date_mismatch"))
          ) {
            case Right(LineupEvent(
              date, Game.LocationType.Home, `mins`, `mins`, 0.0, score,
              TeamSeasonId(TeamId("TeamB"), Year(2018)),
              TeamSeasonId(TeamId("TeamA"), Year(2018)),
              _, lineup, Nil, Nil, Nil, _, _
            )) =>
              date.toString ==> "2018-12-10T17:00:00.000-05:00"
              lineup ==> {
                "S1rname, F1rstname TeamB" ::
                "S2rname, F2rstname TeamB" ::
                "S3rname, F3rstname TeamB" ::
                "S4rname, F4rstname TeamB" ::
                "S5rname, F5rstname TeamB" ::
                "S6rname, F6rstname TeamB" ::
                "S7rname, F7rstname TeamB" ::
                "S8rname, F8rstname TeamB" ::
                "S9rname, F9rstname TeamB" ::
                "SArname, FArstname TeamB" ::
                Nil
              }.map(build_player_code).sortBy(_.code)

              score ==> LineupEvent.ScoreInfo(Game.Score(0,0),Game.Score(91,92),0,0)
          }
          // Neutral
          val adjusted_lineup_html = lineup_html.replace("12/10/2018", "12/10/2018 17:00")
          List(lineup_html, adjusted_lineup_html).foreach { html =>
            TestUtils.inside(
              get_box_lineup(s"test_p$period.html", html, TeamId("TeamA"), Set("12/10/2018"))
            ) {
              case Right(LineupEvent(
                _, Game.LocationType.Neutral, _, _, _, _,
                _,
                _,
                _, _, _, _, _, _, _
              )) =>
            }
          }
        }
      }
      "parse_final_score" - {
        // Some low level checking (HTML parsing is tested by get_lineup)
        TestUtils.inside(parse_final_score(List(), target_team_first = true)) {
          case Left(_) =>
        }
        TestUtils.inside(parse_final_score(List("1"), target_team_first = true)) {
          case Left(_) =>
        }
        TestUtils.inside(parse_final_score(List("1", "2"), target_team_first = true)) {
          case Right(Game.Score(1, 2)) =>
        }
        TestUtils.inside(parse_final_score(List("1", "2", "3"), target_team_first = true)) {
          case Left(_) =>
        }
        TestUtils.inside(parse_final_score(List("1", "2", "3", "4"), target_team_first = false)) {
          case Right(Game.Score(4, 2)) =>
        }
        TestUtils.inside(parse_final_score(List("1", "rabbit", "3", "4"), target_team_first = true)) {
          case Left(_) =>
        }
        TestUtils.inside(parse_final_score(List("1", "2", "3", "4", "5", "6"), target_team_first = true)) {
          case Right(Game.Score(3, 6)) =>
        }
      }
    }
  }
}
