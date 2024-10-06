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

  // Utils for building test data

  def event_time_formatter(in: ShotEvent): ShotEvent =
    in.copy(shot_min = 0.01 * ((in.shot_min * 100.0).toInt))

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

  val base_event_doc = TestUtils.get_doc(
      """
        <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
        """
  )
  val base_event = parse_shot_html(
    v1_builders.shot_event_finder(base_event_doc).head,
    box_lineup,
    v1_builders,
    tidy_ctx,
    target_team_first = true
  ).right.get._2

  ///////////////////////////////////////////////// Tests

  val tests = Tests {
    "ShotEventParser" - {
      "parse_shot_html" - {
        val base_event_1 =
          build_base_event(box_lineup).copy(
            x = 310.2, 
            y = 235, 
            shot_min = 13.08,
            shooter = Some(box_players.head),
            score = Game.Score(9, 6)
          )
        val base_event_2 = build_base_event(box_lineup).copy(
          x = 629.8000000000001, 
          y = 185, 
          shot_min = 4.46,
          score = Game.Score(25, 20),
          is_off = false,
          pts = 1
        )

        case class TestScenario(
          html: String, expected: ShotEvent, expected_period: Int, box: LineupEvent = box_lineup, target: Boolean = true
        )

        val valid_test_inputs = List(
          // Base scanario:
          TestScenario("""
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """, base_event_1, 1),
          // Location swap:
          TestScenario("""
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """, base_event_1.copy(score = Game.Score(6, 9), location_type = Game.LocationType.Away), 
            1, box = box_lineup.copy(location_type = Game.LocationType.Away)),
          TestScenario("""
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """, base_event_1.copy(score = Game.Score(9, 6), location_type = Game.LocationType.Neutral), 
            1, box = box_lineup.copy(location_type = Game.LocationType.Neutral)),
          TestScenario("""
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """, base_event_1.copy(score = Game.Score(6, 9), location_type = Game.LocationType.Neutral), 
            1, box = box_lineup.copy(location_type = Game.LocationType.Neutral), target = false),
          // Weird team name:
          TestScenario("""
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(St. Francis (PA)) 9-6</title></circle>
            """,
            base_event_1.copy(team = TeamSeasonId(TeamId("St. Francis (PA)"), Year(2023))),
            1,
            box = box_lineup.copy(
              team = TeamSeasonId(TeamId("St. Francis (PA)"), Year(2023))
            )
          ),          
          // Base opponent scenario:
          TestScenario("""
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_1 player_768305790 team_539 shot made"><title>1st 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """, base_event_2, 1),
          // Last one repeated with different periods
          TestScenario("""
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_1 player_768305790 team_539 shot made"><title>2nd 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """, base_event_2, 2),
          TestScenario("""
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_2 player_768305790 team_539 shot made"><title>3rd 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """, base_event_2, 3),
          TestScenario("""
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_3 player_768305790 team_539 shot made"><title>4th 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """, base_event_2, 4),
          TestScenario("""
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_4 player_768305790 team_539 shot made"><title>5th 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """, base_event_2, 5),
        )
        // As above, but missing each of the key fields: cx/cy/title/ the score in title / the time in title / the team in title
        val invalid_test_inputs = List(
          """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="player_768305790 team_539 shot made"><title>04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """ -> "[0]", // (period)
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st: missed by Jahari Long(Maryland) 9-6</title></circle>
            """ -> "[0,1]", // (time; use the time to find the period so if one is missing so is the other)
          """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_4 player_768305790 team_539 shot made"><title>4th 04:28:00 : made by (Penn St.) 25-20</title></circle>
            """ -> "[2]", // (player)
          """
            <circle cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """ -> "[3]", // (location)
          """
            <circle cx="310.2" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """ -> "[3]", // (location)
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland)</title></circle>
            """ -> "[4,6]", // (score; use the score to find the team so if one is missing so is the other)
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot"><title>1st 13:05:00 : taken by Jahari Long(Maryland) 9-6</title></circle>
            """ -> "[2,5]", // (shot result; use the shot result to find the player so if one is missing so is the other)
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long 9-6</title></circle>
            """ -> "[2,6]", // (team; use the team to find the player so if one is missing so is the other)
          """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"></circle>
            """ -> "[0,1,2,4,5,6]" // (if title is missing, that's all fields exception location)
        )

        valid_test_inputs.zipWithIndex.foreach {
          case (scenario, test_num) =>
            TestUtils.with_doc(scenario.html) { doc =>
              TestUtils.inside(v1_builders.shot_event_finder(doc) -> test_num) {
                case (List(element), test_case) =>
                  val result = parse_shot_html(
                    element,
                    scenario.box,
                    v1_builders,
                    tidy_ctx,
                    scenario.target
                  )
                  TestUtils.inside(result) {
                    case Right((shot_period, shot_event))
                        if event_time_formatter(
                          shot_event
                        ) == event_time_formatter(
                          scenario.expected
                        ) && shot_period == scenario.expected_period =>

                    case Right((shot_period, shot_event)) =>
                      println(
                        s"************** MISMATCH SHOT EVENT [$test_case] **************"
                      )
                      println(s"E [${scenario.expected_period}]: ${event_time_formatter(scenario.expected)}")
                      println(s" vs ")
                      println(s"A [$shot_period]: ${event_time_formatter(shot_event)}")
                      assert(false)

                  }
              }
            }
        }

        invalid_test_inputs.zipWithIndex.foreach {
          case ((input, error_str), test_num) =>
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
                  assert(result.left.get.toString.contains(error_str))
              }
            }
        }
      }

      "is_women_game" - {
        case class TestScenario(in: List[(Int, ShotEvent)], expected: Boolean)

        val test_scenarios = List(
          TestScenario(
            List(1 -> base_event.copy(shot_min = 9.0)), false // (not enough periods)
          ),
          TestScenario(
            List(
              1 -> base_event.copy(shot_min = 11.0), //shot taken before quarter
              2 -> base_event.copy(shot_min = 9.0),
              3 -> base_event.copy(shot_min = 9.0),
              4 -> base_event.copy(shot_min = 9.0),
              ), false // (shot taken before quarter)
          ),
          TestScenario(
            List(
              1 -> base_event.copy(shot_min = 9.0), 
              2 -> base_event.copy(shot_min = 9.0),
              3 -> base_event.copy(shot_min = 9.0),
              3 -> base_event.copy(shot_min = 9.0),
              ), false // (not enough periods)
          ),
          TestScenario(
            List(
              1 -> base_event.copy(shot_min = 9.0), 
              2 -> base_event.copy(shot_min = 9.0),
              3 -> base_event.copy(shot_min = 9.0),
              4 -> base_event.copy(shot_min = 9.0),
              ), true
          )
        )
        test_scenarios.foreach { scenario =>
          assert(is_women_game(scenario.in) == scenario.expected)
        }
      }
      "get_ascending_time" - {
        // Women's games
        List(
          (get_ascending_time(base_event.copy(shot_min = 4.0), 1, true), 6.0),
          (get_ascending_time(base_event.copy(shot_min = 6.0), 2, true), 14.0),
          (get_ascending_time(base_event.copy(shot_min = 1.0), 3, true), 29.0),
          (get_ascending_time(base_event.copy(shot_min = 8.0), 4, true), 32.0),
          (get_ascending_time(base_event.copy(shot_min = 2.0), 5, true), 43.0),
          (get_ascending_time(base_event.copy(shot_min = 0.0), 6, true), 50.0)
        ).foreach {
          TestUtils.inside(_) {
            case (result, expected) =>
              assert(result == expected)
          }
        }
        // Men's games
        List(
          (get_ascending_time(base_event.copy(shot_min = 4.0), 1, false), 16.0),
          (get_ascending_time(base_event.copy(shot_min = 6.0), 2, false), 34.0),
          (get_ascending_time(base_event.copy(shot_min = 1.0), 3, false), 44),
          (get_ascending_time(base_event.copy(shot_min = 4.0), 4, false), 46.0)
        ).foreach {
          TestUtils.inside(_) {
            case (result, expected) =>
              assert(result == expected)
          }
        }
      }
      "is_team_shooting_left_to_start" - {
      }
      "phase1_shot_event_enrichment" - {
        val base_event_1 = build_base_event(box_lineup).copy()

        //TODO
      }
    }
  }

}
