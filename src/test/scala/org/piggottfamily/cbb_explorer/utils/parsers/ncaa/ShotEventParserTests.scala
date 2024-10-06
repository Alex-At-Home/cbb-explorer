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
import cats.implicits._
import cats.data._
import com.github.dwickern.macros.NameOf._
import shapeless._
import ops.hlist._
import shapeless.labelled._
import record._
import ops.record._
import syntax.singleton._
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.Try

object ShotEventParserTests extends TestSuite with ShotEventParser {
  import ExtractorUtils._
  import ExtractorUtilsTests._

  val tests = Tests {
    "ShotEventParser" - {
      "parse_shot_html" - {
        val valid_test_inputs = List(
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """,
          // TODO: check this one gets the team right
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(St. Francis (PA))) 9-6</title></circle>
            """,
          """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_1 player_768305790 team_539 shot made"><title>1st 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
          // Last one repeated with different periods
          """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_1 player_768305790 team_539 shot made"><title>1st 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
          """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_2 player_768305790 team_539 shot made"><title>2nd 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
          """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_3 player_768305790 team_539 shot made"><title>3rd 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
          """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_4 player_768305790 team_539 shot made"><title>4th 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """
        )
        // As above, but missing each of the key fields: cx/cy/title/ the score in title / the time in title / the team in title
        val invalid_test_inputs = List(
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot"><title>1st 13:05:00 : taken by Jahari Long(Maryland) 9-6</title></circle>
            """,
          """
            <circle cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """,
          """
            <circle cx="310.2" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """,
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"></circle>
            """,
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland)</title></circle>
            """,
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st: missed by Jahari Long(Maryland)</title></circle>
            """,
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long 9-6</title></circle>
            """,
          """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="player_768305790 team_539 shot made"><title>04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """
        )

        val box_players = List("Long, Jahari").map(build_player_code(_, None))

        val box_lineup = LineupEvent(
          date = new DateTime(),
          location_type = Game.LocationType.Home,
          start_min = 0.0,
          end_min = -100.0,
          duration_mins = 0.0,
          score_info = LineupEvent.ScoreInfo.empty,
          team = TeamSeasonId(TeamId("Maryland"), Year(2023)),
          opponent = TeamSeasonId(TeamId("Penn St."), Year(2023)),
          lineup_id = LineupEvent.LineupId.unknown,
          players = box_players,
          players_in = Nil,
          players_out = Nil,
          raw_game_events = Nil,
          team_stats = LineupEventStats.empty,
          opponent_stats = LineupEventStats.empty
        )
        val tidy_ctx =
          LineupErrorAnalysisUtils.build_tidy_player_context(box_lineup)

        valid_test_inputs.zipWithIndex.foreach { case (input, test_num) =>
          TestUtils.with_doc(input) { doc =>
            TestUtils.inside(v1_builders.shot_event_finder(doc) -> test_num) {
              case (List(element), test_case) =>
                val result = parse_shot_html(
                  element,
                  box_lineup,
                  v1_builders,
                  tidy_ctx,
                  target_team_first = true
                )
                assert(result.isRight)
            }
          }
        }

        invalid_test_inputs.zipWithIndex.foreach { case (input, test_num) =>
          TestUtils.with_doc(input) { doc =>
            TestUtils.inside(v1_builders.shot_event_finder(doc) -> test_num) {
              case (List(element), test_case) =>
                val result = parse_shot_html(
                  element,
                  box_lineup,
                  v1_builders,
                  tidy_ctx,
                  target_team_first = true
                )
                assert(result.isLeft)
            }
          }
        }
      }
    }
  }

}
