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

  def double_formatter(in: Double): Double = {
    0.01 * ((in * 100.0).toInt)
  }
  def event_formatter(in: ShotEvent): ShotEvent =
    in.copy(
      shot_min = double_formatter(in.shot_min),
      x = double_formatter(in.x),
      y = double_formatter(in.y),
      dist = double_formatter(in.dist)
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
      "shot_js_to_html" - {
        val script_html = """
          <script>
            def addShot(x, y, z, blah, title, etc) {}
          </script>
          <script>
            addShot(27.0, 77.0, 392, false, 2587511021, '1st 19:26:00 : missed by Donta Scott(Maryland) 0-0', 'period_1 player_769731016 team_392', false); //2587511021, 11
            addShot(92.0, 55.0, 796, false, 2587511023, '1st 18:55:00 : missed by Tyler Wahl(Wisconsin) 0-0', 'period_1 player_769731004 team_796', false); //2587511023, 13
            addShot(90.0, 64.0, 796, false, 2587511028, '1st 18:22:00 : missed by Steven Crowl(Wisconsin) 0-0', 'period_1 player_769731006 team_796', false); //2587511028, 20
            addShot(94.0, 53.0, 450, false, 2552825138, '1st 13:10:00 : missed by De&#39;Shayne Montgomery(Mount St. Mary&#39;s) 4-7', 'period_1 player_767427911 team_450', false); //2552825138, 116
          </script>
        """
        val script_doc = TestUtils.get_doc(script_html)
        TestUtils.inside(v1_builders.script_extractor(script_doc)) {
          case Some(script_str) =>
            val results =
              shot_js_to_html(script_str, v1_builders, JsoupBrowser()).map {
                el =>
                  (
                    v1_builders.shot_location_finder(el),
                    v1_builders.event_player_finder(el)
                  )
              }
            assert(
              results == List(
                (Some((253.8, 385.0)), Some("Scott, Donta")),
                (Some((864.8000000000001, 275.0)), Some("Wahl, Tyler")),
                (Some((846.0, 320.0)), Some("Crowl, Steven")),
                (Some((883.6, 265.0)), Some("Montgomery, De'Shayne"))
              )
            )
        }
      }
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
          shooter =
            Some(LineupEvent.PlayerCodeId("KaClary", PlayerId("Clary, Kanye"))),
          score = Game.Score(25, 20),
          is_off = false,
          pts = 1
        )

        case class TestScenario(
            html: String,
            expected: ShotEvent,
            expected_period: Int,
            box: LineupEvent = box_lineup,
            target: Boolean = true
        )

        val valid_test_inputs = List(
          // Base scanario:
          TestScenario(
            """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """,
            base_event_1,
            1
          ),
          // Location swap:
          TestScenario(
            """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """,
            base_event_1.copy(
              score = Game.Score(6, 9),
              location_type = Game.LocationType.Away
            ),
            1,
            box = box_lineup.copy(location_type = Game.LocationType.Away)
          ),
          TestScenario(
            """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """,
            base_event_1.copy(
              score = Game.Score(9, 6),
              location_type = Game.LocationType.Neutral
            ),
            1,
            box = box_lineup.copy(location_type = Game.LocationType.Neutral)
          ),
          TestScenario(
            """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(Maryland) 9-6</title></circle>
            """,
            base_event_1.copy(
              score = Game.Score(6, 9),
              location_type = Game.LocationType.Neutral
            ),
            1,
            box = box_lineup.copy(location_type = Game.LocationType.Neutral),
            target = false
          ),
          // Weird team name:
          TestScenario(
            """
            <circle cx="310.2" cy="235" r="5" style="fill: white; stroke: blue; stroke-width: 3px; display: inline;" id="play_2565239320" class="period_1 player_768305773 team_392 shot missed"><title>1st 13:05:00 : missed by Jahari Long(St. Francis (PA)) 9-6</title></circle>
            """,
            base_event_1.copy(team =
              TeamSeasonId(TeamId("St. Francis (PA)"), Year(2023))
            ),
            1,
            box = box_lineup.copy(
              team = TeamSeasonId(TeamId("St. Francis (PA)"), Year(2023))
            )
          ),
          // Base opponent scenario:
          TestScenario(
            """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_1 player_768305790 team_539 shot made"><title>1st 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
            base_event_2,
            1
          ),
          // Last one repeated with different periods
          TestScenario(
            """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_1 player_768305790 team_539 shot made"><title>2nd 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
            base_event_2,
            2
          ),
          TestScenario(
            """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_2 player_768305790 team_539 shot made"><title>3rd 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
            base_event_2,
            3
          ),
          TestScenario(
            """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_3 player_768305790 team_539 shot made"><title>4th 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
            base_event_2,
            4
          ),
          TestScenario(
            """
            <circle cx="629.8000000000001" cy="185" r="5" style="fill: grey; stroke: grey; stroke-width: 3px; display: inline;" id="play_2565239462" class="period_4 player_768305790 team_539 shot made"><title>5th 04:28:00 : made by Kanye Clary(Penn St.) 25-20</title></circle>
            """,
            base_event_2,
            5
          )
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

        valid_test_inputs.zipWithIndex.foreach { case (scenario, test_num) =>
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
                      if event_formatter(
                        shot_event
                      ) == event_formatter(
                        scenario.expected
                      ) && shot_period == scenario.expected_period =>

                  case Right((shot_period, shot_event)) =>
                    println(
                      s"************** MISMATCH SHOT EVENT [$test_case] **************"
                    )
                    println(
                      s"E [${scenario.expected_period}]: ${event_formatter(scenario.expected)}"
                    )
                    println(s" vs ")
                    println(s"A [$shot_period]: ${event_formatter(shot_event)}")
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
            List(1 -> base_event.copy(shot_min = 9.0)),
            false // (not enough periods)
          ),
          TestScenario(
            List(
              1 -> base_event.copy(shot_min =
                11.0
              ), // shot taken before quarter
              2 -> base_event.copy(shot_min = 9.0),
              3 -> base_event.copy(shot_min = 9.0),
              4 -> base_event.copy(shot_min = 9.0)
            ),
            false // (shot taken before quarter)
          ),
          TestScenario(
            List(
              1 -> base_event.copy(shot_min = 9.0),
              2 -> base_event.copy(shot_min = 9.0),
              3 -> base_event.copy(shot_min = 9.0),
              3 -> base_event.copy(shot_min = 9.0)
            ),
            false // (not enough periods)
          ),
          TestScenario(
            List(
              1 -> base_event.copy(shot_min = 9.0),
              2 -> base_event.copy(shot_min = 9.0),
              3 -> base_event.copy(shot_min = 9.0),
              4 -> base_event.copy(shot_min = 9.0)
            ),
            true
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
          TestUtils.inside(_) { case (result, expected) =>
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
          TestUtils.inside(_) { case (result, expected) =>
            assert(result == expected)
          }
        }
      }
      "is_team_shooting_left_to_start" - {
        val test_case_1 = List(
          1 -> base_event,
          1 -> base_event,
          1 -> base_event,
          1 -> base_event.copy(x = 1000),
          1 -> base_event.copy(
            x = 1000,
            is_off = false
          ), // (these will all be ignored because they are not offensive)
          1 -> base_event.copy(x = 1000, is_off = false),
          1 -> base_event.copy(x = 1000, is_off = false),
          1 -> base_event.copy(x = 1000, is_off = false),
          1 -> base_event.copy(x = 1000, is_off = false),
          1 -> base_event.copy(x = 1000, is_off = false),
          2 -> base_event.copy(x =
            1000
          ), // (these will all be ignored because they are in period 2)
          2 -> base_event.copy(x = 1000),
          2 -> base_event.copy(x = 1000),
          2 -> base_event.copy(x = 1000),
          2 -> base_event.copy(x = 1000),
          2 -> base_event.copy(x = 1000)
        )
        assert(is_team_shooting_left_to_start(test_case_1) == (true, 1))

        val test_case_2 = test_case_1.map { case (period, event) =>
          if (event.x > 800)
            (period, event.copy(x = 300))
          else
            (period, event.copy(x = 1000))
        }
        assert(is_team_shooting_left_to_start(test_case_2) == (false, 1))
      }
      "phase1_shot_event_enrichment/transform_shot_location" - {

        val test_case = List(
          1 -> base_event,
          1 -> base_event.copy(
            x = ShotMapDimensions.court_length_x_px - base_event.x,
            y = ShotMapDimensions.court_width_y_px - base_event.y,
            is_off = false // (will convert back to the same location)
          ),
          2 -> base_event.copy(
            x = ShotMapDimensions.court_length_x_px - base_event.x,
            y = ShotMapDimensions.court_width_y_px - base_event.y
          ), // (2nd period so will be the same location)
          2 -> base_event.copy(
            x = base_event.x,
            y = base_event.y,
            is_off = false // (2nd period so will be the same location)
          )
        )

        // (reminder pixel x and y are: cx="310.2" cy="235")
        val transformed_base_event = event_formatter(
          base_event.copy(
            x = 26.02, // TODO is this right?
            y = 1.5,
            dist = 26.060000000000002
          )
        )
        TestUtils.inside(
          phase1_shot_event_enrichment(test_case).map(event_formatter)
        ) { case List(t_event_1, t_event_2, t_event_3, t_event_4) =>
          assert(t_event_1 == transformed_base_event.copy(shot_min = 6.91))
          assert(
            t_event_2 == transformed_base_event.copy(
              shot_min = 6.91,
              is_off = false
            )
          )
          assert(t_event_3 == transformed_base_event.copy(shot_min = 26.91))
          assert(
            t_event_4 == transformed_base_event.copy(
              shot_min = 26.91,
              is_off = false
            )
          )
        }
        TestUtils.inside(
          transform_shot_location(
            x = ShotMapDimensions.court_length_x_px - base_event.x,
            y = ShotMapDimensions.court_width_y_px - base_event.y,
            period_delta = 0, // (1st period)
            team_shooting_left_in_first_period =
              false, // (just want to check this case)
            is_offensive = true
          )
        ) { case (result_x, result_y) =>
          assert(double_formatter(result_x) == transformed_base_event.x)
          assert(double_formatter(result_y) == transformed_base_event.y)
        }
      }
    }
  }
}
