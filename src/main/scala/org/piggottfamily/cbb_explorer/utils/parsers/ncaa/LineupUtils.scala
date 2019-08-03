package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

trait LineupUtils {
  import ExtractorUtils._

  /** TODO build the stats from the game events */
  def enrich_lineup(lineup: LineupEvent): LineupEvent = {
    val scored = lineup.score_info.end.scored - lineup.score_info.start.scored
    val allowed = lineup.score_info.end.allowed - lineup.score_info.start.allowed
    val (team_possessions, opp_possessions) = calculate_possessions(lineup.raw_game_events)
    lineup.copy(
      team_stats = lineup.team_stats.copy(
        num_events = lineup.raw_game_events.filter(_.team.isDefined).size,
        num_possessions = team_possessions,
        pts = scored,
        plus_minus = scored - allowed
      ),
      opponent_stats = lineup.opponent_stats.copy(
        num_events = lineup.raw_game_events.filter(_.opponent.isDefined).size, //TODO exclude subs
        num_possessions = opp_possessions,
        pts = allowed,
        plus_minus = allowed - scored
      )
    )
  }

  /** Returns team and opponent possessions based on the raw event data */
  protected def calculate_possessions(raw_events: Seq[LineupEvent.RawGameEvent]): (Int, Int) = {
    def is_substitution_event(event_str: String): Boolean = {
      //TOOD: should use PlayByPlayParser.ParseTeamSubIn and PlayByPlayParser.ParseTeamSubOut)
      val normalized_event_str = event_str.toLowerCase()

      normalized_event_str.contains("substitution in") ||
      normalized_event_str.contains("substitution out") ||
      normalized_event_str.contains("enters game") ||
      normalized_event_str.contains("leaves game")
    }
    def is_ignorable_game_event(event_str: String): Boolean = {
      val normalized_event_str = event_str.toLowerCase()
      false
    }
    def is_ignorable_event(event_str: String): Boolean =
      is_substitution_event(event_str) || is_ignorable_game_event(event_str)

    object Direction extends Enumeration {
      val Init, Team, Opponent = Value
    }
    case class PossState(team: Int, opponent: Int, direction: Direction.Value)

    (raw_events.foldLeft(PossState(0, 0, Direction.Init)) {
      case (state, LineupEvent.RawGameEvent(_, Some(opp_info))) if is_ignorable_event(opp_info) =>
        state //(ignore sub data)
      case (state @ PossState(_, _, Direction.Init), LineupEvent.RawGameEvent(None, Some(opp_info))) =>
        state.copy(opponent = 1, direction = Direction.Opponent)
      case (state @ PossState(_, _, Direction.Init), LineupEvent.RawGameEvent(Some(team_info), None)) =>
        state.copy(team = 1, direction = Direction.Team)
      case (state @ PossState(_, opp_poss, Direction.Team), LineupEvent.RawGameEvent(None, Some(opp_info))) =>
        state.copy(opponent = opp_poss + 1, direction = Direction.Opponent)
      case (state @ PossState(team_poss, _, Direction.Opponent), LineupEvent.RawGameEvent(Some(team_info), None)) =>
        state.copy(team = team_poss + 1, direction = Direction.Team)
      case (state, _) =>
        state //(all other cases just do nothing)
    }) match {
      case PossState(team, opp, _) => (team, opp)
    }
  }
}
object LineupUtils extends LineupUtils

//TODO: I should probably be building "possession events" so that I can
// a) validate the results
// b) build a separate possession events table

/*
EXAMPLE PROBLEM: THIS ISN'T 2 POSSESSIONS I THINK, THE BLOCK RESULTS IN AN ORB?
RawGameEvent(Some("14:11:00,7-9,Darryl Morsell, 2pt layup blocked missed"), None),
RawGameEvent(None, Some("14:11:00,7-9,Emmitt Williams, block")),
RawGameEvent(Some("14:11:00,7-9,Team, rebound offensivedeadball"), None),
RawGameEvent(Some("14:00:00,7-9,Anthony Cowan, 3pt jumpshot 2ndchance missed"), None),

LEGACY
"team": "04:53,55-69,LAYMAN,JAKE Blocked Shot"
"opponent": "04:53,55-69,TEAM Offensive Rebound"
NOTE: check out the UMD-Kansas 2015 season ... all the blocks seem to
      result in a change of event (with the next even missing from the data)
      AND it's unclear why the next event is corrupt, eye balling it .. looks fine
      https://stats.ncaa.org/game/play_by_play/4077212
*/

/*
ANOTHER PROBLEM ... A FOUL DOESN'T CHANGE POSSESSIONS
RawGameEvent(None, Some("13:36:00,7-9,Team, rebound offensivedeadball")),
RawGameEvent(Some("13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow"), None)

LEGACY:
"opponent": "10:00,51-60,MYKHAILIUK,SVI Commits Foul"
*/

/*
AND ANOTHER ONE ... JUMP BALLS
RawGameEvent(None, Some("19:58:00,0-0,Kavell Bigby-Williams, jumpball lost")),
RawGameEvent(Some("19:58:00,0-0,Bruno Fernando, jumpball won"), None),

(no legacy equivalent)
*/
