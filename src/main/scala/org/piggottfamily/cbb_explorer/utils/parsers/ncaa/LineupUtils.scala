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

    object Direction extends Enumeration {
      val Init, Team, Opponent = Value
    }
    case class PossState(team: Int, opponent: Int, direction: Direction.Value)

    (raw_events.foldLeft(PossState(0, 0, Direction.Init)) {
      case (state, LineupEvent.RawGameEvent(_, Some(opp_info))) if is_substitution_event(opp_info) =>
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
