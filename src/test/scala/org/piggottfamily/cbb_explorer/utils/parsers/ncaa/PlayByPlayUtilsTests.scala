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

object PlayByPlayUtilsTests extends TestSuite with PlayByPlayUtils {
  import ExtractorUtils._
  import ExtractorUtilsTests._

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

  val base_shot_event = ShotEvent(
    shooter = None,
    date = new DateTime(),
    location_type = Game.LocationType.Home,
    team = TeamSeasonId(TeamId("Maryland"), Year(2023)),
    opponent = TeamSeasonId(TeamId("Penn St."), Year(2023)),
    is_off = false,
    lineup_id = LineupEvent.LineupId.unknown,
    players = box_players,
    score = Game.Score(0, 0),
    shot_min = 0.0,
    x = 0.0,
    y = 0.0,
    dist = 0.0,
    pts = 2,
    value = 2,
    assisted_by = None,
    is_assisted = None,
    in_transition = None
  )
  val base_team_pbp =
    Model.OtherTeamEvent(
      min = 5.0,
      score = Game.Score(0, 0),
      event_string =
        "18:28:00,0-0,Kyle Guy, assist" // (just needs to extra to game event)
    )
  val base_oppo_pbp =
    Model.OtherOpponentEvent(
      min = 5.0,
      score = Game.Score(0, 0),
      event_string =
        "18:28:00,0-0,Kyle Guy, assist" // (just needs to extra to game event)
    )
  val base_misc_pbp_ev =
    Model.GameBreakEvent(min = 5.0, score = Game.Score(0, 0))

  val tests = Tests {
    "PlayByPlayUtils" - {
      "ShotEnrichmentUtils" - {
        import ShotEnrichmentUtils._

        "find_lineup(curr_pbp = None)" - {
          val lineup1 = box_lineup.copy(start_min = 0.0, end_min = 5.0)
          val lineup2 = box_lineup.copy(start_min = 5.0, end_min = 10.0)
          val lineup3 = box_lineup.copy(start_min = 10.0, end_min = 15.0)
          val lineup_post_gap =
            box_lineup.copy(start_min = 30.0, end_min = 35.0)
          case class Scenario(
              shot: ShotEvent,
              expected_lineup: Option[LineupEvent],
              expected_iterator_remaining: Boolean = true,
              expected_stash: List[LineupEvent] = Nil
          )
          val test_scenarios =
            List(
              Scenario(
                base_shot_event.copy(shot_min = 0.0),
                Some(lineup1)
              ),
              Scenario(
                base_shot_event.copy(shot_min = 1.0),
                Some(lineup1)
              ),
              Scenario(
                base_shot_event.copy(shot_min = 5.0),
                Some(lineup1)
              ),
              Scenario(
                base_shot_event.copy(shot_min = 5.1),
                Some(lineup2)
              ),
              Scenario(
                base_shot_event.copy(shot_min = 10.0),
                Some(lineup2)
              ),
              Scenario(
                base_shot_event.copy(shot_min = 10.5),
                Some(lineup3)
              ),
              Scenario(
                base_shot_event.copy(shot_min = 15.0),
                Some(lineup3)
              ),
              Scenario(
                base_shot_event.copy(shot_min = 15.0),
                Some(lineup3)
              ),
              Scenario(
                base_shot_event.copy(shot_min = 25.0),
                expected_lineup = None,
                expected_stash = List(lineup_post_gap),
                expected_iterator_remaining = false
              ),
              Scenario(
                base_shot_event.copy(shot_min = 40.0),
                expected_lineup = None,
                expected_iterator_remaining = false
              )
            )
          test_scenarios.foreach { scenario =>
            List(true, false).foreach { is_off =>
              val direct_result = find_lineup(
                scenario.shot.copy(is_off = is_off),
                curr_pbp = None,
                scenario.expected_lineup.toList,
                Iterator()
              )
              assert(
                direct_result == (scenario.expected_lineup, scenario.expected_lineup.toList)
              )
              // (never a stash with direct result, since there is no iterator to pull off)

              val lineup_it =
                Iterator(lineup1, lineup2, lineup3, lineup_post_gap);
              val iterating_result = find_lineup(
                scenario.shot.copy(is_off = is_off),
                curr_pbp = None,
                curr_lineups = Nil,
                lineup_it
              )
              assert(
                iterating_result == (scenario.expected_lineup, scenario.expected_lineup.toList ++ scenario.expected_stash)
              )
              // Check iterator status:
              assert(
                lineup_it.hasNext == scenario.expected_iterator_remaining
              )

              // Search vs the stash:
              val stash = List(lineup1, lineup2, lineup3, lineup_post_gap)
              val stashing_lineup_it =
                Iterator(lineup_post_gap) // (should remain untouched)
              val stashing_result = find_lineup(
                scenario.shot.copy(is_off = is_off),
                curr_pbp = None,
                curr_lineups = stash,
                lineup_it = stashing_lineup_it
              )
              val remaining_stash = scenario.expected_lineup match {
                case Some(lineup) =>
                  stash.dropWhile(_.end_min <= lineup.end_min)
                case None if scenario.expected_stash.nonEmpty => // (in the gap)
                  scenario.expected_stash
                case _ => Nil // (post gap)
              }
              assert(
                stashing_result == (scenario.expected_lineup, scenario.expected_lineup.toList ++ remaining_stash)
              )
              assert( // (check the iterator is only used at the very end, until then only the stash)
                stashing_lineup_it.hasNext == scenario.shot.shot_min < 40.0
              )
            }
          }
        }
        "find_lineup(curr_pbp = Some)" - {

          // Scenario 1: event is first encountered
          val scenario_1_lineup_1 = box_lineup.copy(
            start_min = 0.0,
            end_min = 5.0,
            raw_game_events = List(
              LineupEvent.RawGameEvent(5.0, Some(base_team_pbp.event_string))
            )
          )
          val scenario_1_it = List(scenario_1_lineup_1, box_lineup).iterator
          val scenario_1_result = find_lineup(
            base_shot_event.copy(shot_min = 5.0, is_off = true),
            Some(base_team_pbp.copy(min = 5.0)),
            curr_lineups = Nil,
            scenario_1_it
          )
          assert(
            scenario_1_result == (Some(scenario_1_lineup_1), List(
              scenario_1_lineup_1
            ))
          )
          assert(scenario_1_it.next == box_lineup)

          // Scenario 2: event is a subsequent event
          val scenario_2_lineup_1 = box_lineup.copy(
            start_min = 0.0,
            end_min = 5.0,
            raw_game_events = List( // (wrong direction vs shot.is_off)
              LineupEvent.RawGameEvent(5.0, Some(base_team_pbp.event_string))
            )
          )
          val scenario_2_lineup_2 = box_lineup.copy(
            start_min = 5.0,
            end_min = 10.0,
            raw_game_events = List(
              LineupEvent.RawGameEvent(
                5.0,
                None,
                Some(base_team_pbp.event_string)
              )
            )
          )
          val scenario_2_it =
            List(scenario_2_lineup_1, scenario_2_lineup_2, box_lineup).iterator
          val scenario_2_result = find_lineup(
            base_shot_event.copy(shot_min = 5.0, is_off = false),
            Some(base_team_pbp.copy(min = 5.0)),
            curr_lineups = Nil,
            scenario_2_it
          )
          assert(
            scenario_2_result == (Some(scenario_2_lineup_2), List(
              scenario_2_lineup_1,
              scenario_2_lineup_2
            ))
          )
          assert(scenario_2_it.next == box_lineup)

          // Scenario 3: event is not in any, go back to fallback

          val scenario_3_lineup_1 = box_lineup.copy(
            start_min = 0.0,
            end_min = 5.0
          )
          val scenario_3_lineup_2 = box_lineup.copy(
            start_min = 5.0,
            end_min = 10.0
          )
          val scenario_3_lineup_3 = box_lineup.copy(
            start_min = 10.0,
            end_min = 10.0
          )
          val scenario_3_it =
            List(
              scenario_3_lineup_1,
              scenario_3_lineup_2,
              scenario_3_lineup_3
            ).iterator
          val scenario_3_result = find_lineup(
            base_shot_event.copy(shot_min = 5.0, is_off = false),
            Some(base_team_pbp.copy(min = 5.0)),
            curr_lineups = Nil,
            scenario_3_it
          )
          assert(
            scenario_3_result == (Some(scenario_3_lineup_1), List(
              scenario_3_lineup_1,
              scenario_3_lineup_2,
              scenario_3_lineup_3
            ))
          )
        }
        "shot_value" - {
          assert(shot_value("18:28:00,0-0,Kyle Guy, assist") == 0)
          assert(shot_value("18:28:00,0-0,Eric Ayala, 3pt jumpshot made") == 3)
          assert(
            shot_value(
              "18:28:00,0-0,Eric Ayala, 3pt jumpshot 2ndchance missed"
            ) == 3
          )
          assert(
            shot_value(
              "18:28:00,0-0,Jalen Smith, 2pt drivinglayup 2ndchance;pointsinthepaint made"
            ) == 2
          )
          assert(shot_value("18:28:00,0-0,Eric Carter, 2pt layup missed") == 2)
          assert(
            shot_value(
              "18:28:00,0-0,Eric Ayala, 2pt jumpshot 2ndchance missed"
            ) == 2
          )
          assert(
            shot_value("04:28:0,52-59,Team, rebound deadballdeadball") == -1
          )
        }
        "find_pbp_clump" - {
          case class Scenario(
              shot: ShotEvent,
              pbp_curr: List[Model.MiscGameEvent],
              pbp_remaining: List[Model.PlayByPlayEvent],
              next: Option[Model.MiscGameEvent],
              expected: (List[Model.MiscGameEvent], Option[Model.MiscGameEvent])
          )
          val test_scenarios = List(
            Scenario(
              base_shot_event.copy(shot_min = 5.0),
              pbp_curr = Nil,
              pbp_remaining = Nil,
              next = None,
              expected = (Nil, None)
            ),
            Scenario(
              base_shot_event.copy(shot_min = 5.0),
              pbp_curr = Nil,
              pbp_remaining = List(base_team_pbp.copy(min = 10.0)),
              next = None,
              expected = (Nil, Some(base_team_pbp.copy(min = 10.0)))
            ),
            Scenario(
              base_shot_event.copy(shot_min = 5.0),
              pbp_curr = Nil,
              pbp_remaining = List(
                base_team_pbp.copy(min = 2.5),
                base_team_pbp.copy(min = 5.0),
                base_team_pbp.copy(min = 5.0),
                base_team_pbp.copy(min = 10.0)
              ),
              next = None,
              expected = (
                List(
                  base_team_pbp.copy(min = 5.0),
                  base_team_pbp.copy(min = 5.0)
                ),
                Some(base_team_pbp.copy(min = 10.0))
              )
            ),
            Scenario(
              base_shot_event.copy(shot_min = 5.0),
              pbp_curr = Nil,
              pbp_remaining = List(
                base_team_pbp.copy(min = 5.0),
                base_team_pbp.copy(min = 5.0)
              ),
              next = Some(
                base_team_pbp.copy(min = 2.5)
              ),
              expected = (
                List(
                  base_team_pbp.copy(min = 5.0),
                  base_team_pbp.copy(min = 5.0)
                ),
                None
              )
            ),
            Scenario(
              base_shot_event.copy(shot_min = 5.0),
              pbp_curr = List(
                base_team_pbp.copy(min = 5.0),
                base_team_pbp.copy(min = 5.0)
              ),
              pbp_remaining = List(
                base_team_pbp.copy(min = 10.0)
              ),
              next = None,
              expected = (
                List(
                  base_team_pbp.copy(min = 5.0),
                  base_team_pbp.copy(min = 5.0)
                ),
                Some(base_team_pbp.copy(min = 10.0))
              )
            ),
            Scenario(
              base_shot_event.copy(shot_min = 5.0),
              pbp_curr = List(
                base_team_pbp.copy(min = 5.0),
                base_team_pbp.copy(min = 5.0)
              ),
              pbp_remaining = Nil,
              next = None,
              expected = (
                List(
                  base_team_pbp.copy(min = 5.0),
                  base_team_pbp.copy(min = 5.0)
                ),
                None
              )
            )
          )
          test_scenarios.zipWithIndex.foreach { case (scenario, test_num) =>
            val result = find_pbp_clump(
              scenario.shot.shot_min,
              scenario.pbp_remaining.iterator,
              scenario.pbp_curr,
              scenario.next
            )
            val expected = scenario.expected
            Predef.assert( // (needed a custom error message)
              result == expected,
              s"[$test_num]: [$result] != [$expected] ([$scenario])"
            )
          }
        }
        "extract_player_from_ev" - {
          case class Scenario(
              ev_str: String,
              is_off: Boolean,
              expected: Option[LineupEvent.PlayerCodeId]
          )
          val test_scenarios = List(
            Scenario(
              "18:28:00,0-0,Jahar Long, 3pt jumpshot made",
              is_off = true, // (means error above will be corrected)
              expected = Some(
                build_player_code("Long, Jahari", None)
              )
            ),
            Scenario(
              "18:28:00,0-0,Jahar Long, 3pt jumpshot made",
              is_off = false, // (means error above will not be corrected)
              expected = Some(
                build_player_code("Long, Jahar", None)
              )
            )
          )
          test_scenarios.foreach { scenario =>
            val result = extract_player_from_ev(
              base_shot_event.copy(is_off = scenario.is_off),
              base_team_pbp.copy(event_string = scenario.ev_str),
              tidy_ctx
            )
            val expected = scenario.expected
            assert(result == expected)
          }
        }
        "matching_player" - {
          case class Scenario(
              ev_str: String,
              is_off: Boolean,
              expected: Boolean
          )
          val test_scenarios = List(
            Scenario(
              "18:28:00,0-0,Jahar Long, 3pt jumpshot made",
              is_off = true, // (means error above will be corrected)
              expected = true
            ),
            Scenario(
              "18:28:00,0-0,Jahar Long, 3pt jumpshot made",
              is_off = false, // (means error above will not be corrected)
              expected = false
            )
          )
          test_scenarios.foreach { scenario =>
            assert(
              matching_player(
                base_shot_event
                  .copy(
                    is_off = scenario.is_off,
                    shooter = box_players.headOption
                  ),
                base_team_pbp.copy(event_string = scenario.ev_str),
                tidy_ctx
              ) == scenario.expected
            )
          }
        }
        "right_kind_of_shot" - {
          case class Scenario(
              ev_str: String,
              shot: ShotEvent,
              expected: Boolean
          )
          val test_scenarios = List(
            Scenario(
              "18:28:00,0-0,Jahari Long, 3pt jumpshot made",
              shot = base_shot_event.copy(pts = 1, dist = 25.0),
              expected = true
            ),
            Scenario(
              "18:28:00,0-0,Jahari Long, 3pt jumpshot made",
              shot = base_shot_event.copy(pts = 0, dist = 25.0),
              expected = false
            ),
            Scenario(
              "18:28:00,0-0,Jahari Long, 3pt jumpshot missed",
              shot = base_shot_event.copy(pts = 1, dist = 25.0),
              expected = false
            ),
            Scenario(
              "18:28:00,0-0,Jahari Long, 3pt jumpshot missed",
              shot = base_shot_event.copy(pts = 0, dist = 25.0),
              expected = true
            ),
            Scenario(
              "Jahari Long, 2pt jumpshot missed",
              shot = base_shot_event.copy(pts = 0, dist = 25.0),
              expected = false
            ),
            Scenario(
              "Jahari Long, 3pt jumpshot made",
              shot = base_shot_event.copy(pts = 1, dist = 15.0),
              expected = false
            ),
            Scenario(
              "18:28:00,0-0,Jahari Long, 2pt jumpshot made",
              shot = base_shot_event.copy(pts = 1, dist = 15.0),
              expected = true
            )
          )
          test_scenarios.foreach { scenario =>
            assert(
              right_kind_of_shot(
                shot = scenario.shot,
                base_team_pbp.copy(event_string = scenario.ev_str)
              ) == scenario.expected
            )
          }
        }
      }
    }
  }
}
