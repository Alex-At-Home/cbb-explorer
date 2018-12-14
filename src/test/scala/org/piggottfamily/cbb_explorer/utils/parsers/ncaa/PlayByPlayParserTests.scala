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

object PlayByPlayParserTests extends TestSuite with PlayByPlayParser {
  import ExtractorUtils._
  import ExtractorUtilsTests._

  val play_by_play_html = Source.fromURL(getClass.getResource("/ncaa/test_play_by_play.html")).mkString

  val sample_team_sub_in = """
    <table><tr>
      <td class="smtext">15:00</td>
      <td class="smtext">S8RNAME,F8RSTNAME TEAMA Enters Game</td>
      <td class="smtext" align="center">45-26</td>
      <td class="smtext"></td>
    </table></tr>
  """
  val sample_oppo_sub_in = """
    <table><tr>
      <td class="smtext">15:00</td>
      <td class="smtext"></td>
      <td class="smtext" align="center">45-26</td>
      <td class="smtext">S8RNAME,F8RSTNAME TEAMB Enters Game</td>
    </table></tr>
  """

  val sample_team_sub_out = """
    <table><tr>
      <td class="smtext">15:00</td>
      <td class="smtext">S8RNAME,F8RSTNAME TEAMA Leaves Game</td>
      <td class="smtext" align="center">45-26</td>
      <td class="smtext"></td>
    </table></tr>
  """
  val sample_oppo_sub_out = """
    <table><tr>
      <td class="smtext">15:00</td>
      <td class="smtext"></td>
      <td class="smtext" align="center">45-26</td>
      <td class="smtext">S8RNAME,F8RSTNAME TEAMB Leaves Game</td>
    </table></tr>
  """

  val sample_team_event = """
    <table><tr>
      <td class="smtext">15:00</td>
      <td class="smtext">event text</td>
      <td class="smtext" align="center">45-26</td>
      <td class="smtext"></td>
    </table></tr>
  """
  val sample_oppo_event = """
    <table><tr>
      <td class="smtext">15:00</td>
      <td class="smtext"></td>
      <td class="smtext" align="center">45-26</td>
      <td class="smtext">event text</td>
    </table></tr>
  """

  val tests = Tests {
    "PlayByPlayParser" - {

      // Higher level tests

      "create_lineup_data" - {
        val starting_players = {
          "S1rname, F1rstname TeamA" ::
          "S2rname, F2rstname TeamA" ::
          "S3rname, F3rstname TeamA" ::
          "S4rname, F4rstname TeamA" ::
          "S5rname, F5rstname TeamA" ::
          Nil
        }.map(build_player_code).sortBy(_.code)

        val test_lineup = LineupEvent(
          date = new DateTime(),
          start_min = 0.0,
          end_min = -100.0,
          duration_mins = 0.0,
          score_diff = 0,
          team = TeamSeasonId(TeamId("TeamA"), Year(2017)),
          opponent = TeamSeasonId(TeamId("TeamB"), Year(2017)),
          lineup_id = LineupEvent.LineupId.unknown,
          players = starting_players,
          players_in = Nil,
          players_out = Nil,
          raw_team_events = Nil,
          raw_opponent_events = Nil,
          team_stats = LineupEventStats.empty,
          opponent_stats = LineupEventStats.empty
        )
        TestUtils.inside(create_lineup_data("filename_test", play_by_play_html, test_lineup)) {
          case Right((lineup_events, bad_lineup_events)) =>
            lineup_events.size ==> 24 // (by inspection)
            bad_lineup_events.size ==> 4
              //(1 with wrong lineup size, 3 where a benched player was in an event)

            // Spot checks:
            lineup_events.zipWithIndex.foreach { event_index =>
              TestUtils.inside(event_index) {
                case (event, index) =>
                  // Always have 5 players in the lineup:
                  event.players.size ==> 5

                  // Duration is always >0
                  event.duration_mins > 0.0
              }
            }
            // Sum of the durations is the entire game:
            val good_and_bad_events = lineup_events ++ bad_lineup_events
            "%.1f".format(good_and_bad_events.map(_.duration_mins).sum) ==> "45.0"
          }

      }

      "parse_game_events" - {
        TestUtils.inside(parse_game_events("filename_test", play_by_play_html)) {
          case Right(game_events) =>
            game_events.size ==> 567
            // grep -c 'smtext' src/test/resources/ncaa/test_play_by_play.html
            // 2256 # 564*4, ie 564 rows of 4 columns, + 3 game breaks
            game_events.collect {
              case ev: Model.GameBreakEvent => ev
            }.size ==> 2
            game_events.collect {
              case ev: Model.GameEndEvent => ev
            }.size ==> 1
        }
      }

      "enrich_and_reverse_game_events" - {
        val test_list =
          Model.OtherTeamEvent(2.0, "test1") ::
          Model.OtherTeamEvent(3.0, "test2a") ::
          Model.OtherTeamEvent(2.0, "test2b") ::
          Model.OtherTeamEvent(2.5, "test3") ::
          Nil
        TestUtils.inside(enrich_and_reverse_game_events(test_list)) {
          case List(
            Model.GameEndEvent(end_t),
            Model.OtherTeamEvent(game_t_3, "test3"),
            Model.GameBreakEvent(mid_t_2),
            Model.OtherTeamEvent(game_t_2b, "test2b"),
            Model.OtherTeamEvent(game_t_2a, "test2a"),
            Model.GameBreakEvent(mid_t_1),
            Model.OtherTeamEvent(game_t_1, "test1")
          ) =>
          "%.1f".format(game_t_1) ==> "18.0"
          "%.1f".format(mid_t_1) ==> "20.0"
          "%.1f".format(game_t_2a) ==> "37.0"
          "%.1f".format(game_t_2b) ==> "38.0"
          "%.1f".format(mid_t_2) ==> "40.0"
          "%.1f".format(game_t_3) ==> "42.5"
          "%.1f".format(end_t) ==> "45.0"
        }
      }

      // Lower level tests

      "parse_game_score" - {
        TestUtils.with_doc(sample_team_event) { doc =>
          TestUtils.inside(parse_game_score(doc.body)) {
            case Right("45-26") =>
          }
        }
      }
      "parse_desc_game_time" - {
        TestUtils.with_doc(sample_team_event) { doc =>
          TestUtils.inside(parse_desc_game_time(doc.body)) {
            case Right(("15:00", t)) =>
              "%.1f".format(t) ==> "15.0"
          }
        }
      }
      "parse_game_event" - {
        TestUtils.with_doc(sample_team_event) { doc =>
          TestUtils.inside(parse_game_event(doc.body)) {
            case Right(Model.OtherTeamEvent(t, "15:00,45-26,event text")) =>
              "%.1f".format(t) ==> "15.0"
          }
        }
        TestUtils.with_doc(sample_oppo_event) { doc =>
          TestUtils.inside(parse_game_event(doc.body)) {
            case Right(Model.OtherOpponentEvent(t, "15:00,45-26,event text")) =>
              "%.1f".format(t) ==> "15.0"
          }
        }
        TestUtils.with_doc(sample_team_sub_in) { doc =>
          TestUtils.inside(parse_game_event(doc.body)) {
            case Right(Model.SubInEvent(t, "S8RNAME,F8RSTNAME TEAMA")) =>
              "%.1f".format(t) ==> "15.0"
          }
        }
        TestUtils.with_doc(sample_oppo_sub_in) { doc =>
          TestUtils.inside(parse_game_event(doc.body)) {
            case Right(Model.OtherOpponentEvent(t, "15:00,45-26,S8RNAME,F8RSTNAME TEAMB Enters Game")) =>
              "%.1f".format(t) ==> "15.0"
          }
        }
        TestUtils.with_doc(sample_team_sub_out) { doc =>
          TestUtils.inside(parse_game_event(doc.body)) {
            case Right(Model.SubOutEvent(t, "S8RNAME,F8RSTNAME TEAMA")) =>
              "%.1f".format(t) ==> "15.0"
          }
        }
        TestUtils.with_doc(sample_oppo_sub_out) { doc =>
          TestUtils.inside(parse_game_event(doc.body)) {
            case Right(Model.OtherOpponentEvent(t, "15:00,45-26,S8RNAME,F8RSTNAME TEAMB Leaves Game")) =>
              "%.1f".format(t) ==> "15.0"
          }
        }
      }
    }
  }
}
