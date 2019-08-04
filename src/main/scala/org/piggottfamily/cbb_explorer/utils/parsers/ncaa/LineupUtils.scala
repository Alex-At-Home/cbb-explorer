package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

/** Utilities related to building up the lineup stats */
trait LineupUtils {
  import ExtractorUtils._

  /** TODO build the stats from the game events */
  def enrich_lineup(lineup: LineupEvent): LineupEvent = {
    val scored = lineup.score_info.end.scored - lineup.score_info.start.scored
    val allowed = lineup.score_info.end.allowed - lineup.score_info.start.allowed
    val (team_possessions, opp_possessions, proc_events) = calculate_possessions(lineup.raw_game_events)
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
      ),
      raw_game_events = proc_events
    )
  }

  /** Returns team and opponent possessions based on the raw event data */
  protected def calculate_possessions(
    raw_events: Seq[LineupEvent.RawGameEvent]
  ): (Int, Int, List[LineupEvent.RawGameEvent]) =
  {
    def is_substitution_event(event_str: String): Boolean = {
      (Some(event_str), None) match {
        case EventUtils.ParseTeamSubIn(_) => true
        case EventUtils.ParseTeamSubOut(_) => true
        case _ => false
      }
    }
    def is_ignorable_game_event(event_str: String): Boolean = Some(event_str) match {

      // Different cases:
      // 1] Jump ball
      case EventUtils.ParseJumpballWonOrLost(_) => true

      // 2] Blocks (wait for rebound to decide if the possession changes)
      // New:
      //RawGameEvent(Some("14:11:00,7-9,Darryl Morsell, 2pt layup blocked missed"), None),
      //RawGameEvent(None, Some("14:11:00,7-9,Emmitt Williams, block")),
      //RawGameEvent(Some("14:11:00,7-9,Team, rebound offensivedeadball"), None),
      // Legacy:
      //"team": "04:53,55-69,LAYMAN,JAKE Blocked Shot"
      //"opponent": "04:53,55-69,TEAM Offensive Rebound"
      case EventUtils.ParseShotBlocked(_) => true

      // 3] Fouls
      // New:
      //RawGameEvent(None, Some("13:36:00,7-9,Team, rebound offensivedeadball")),
      //RawGameEvent(Some("13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow"), None)
      // Legacy:
      //"opponent": "10:00,51-60,MYKHAILIUK,SVI Commits Foul"
      case EventUtils.ParsePersonalFoul(_) => true

      // 3b] Technical fouls
      // We're going to treat the technical foul like an additional possession
      // because otherwise it's going to be complicated
      case EventUtils.ParseTechnicalFoul(_) => true
      
      case _ => false
    }

    def is_ignorable_event(event_str: String): Boolean =
      is_substitution_event(event_str) || is_ignorable_game_event(event_str)

    object Direction extends Enumeration {
      val Init, Team, Opponent = Value
    }
    case class PossState(
      team: Int, opponent: Int,
      events: List[LineupEvent.RawGameEvent],
      direction: Direction.Value
    )

    /** Adds the possession count to the event */
    def enrich(state: PossState, ev: LineupEvent.RawGameEvent): PossState = {
      val enriched_event = ev.copy(
        team_possession = if (state.direction == Direction.Team) Some(state.team) else None,
        opponent_possession = if (state.direction == Direction.Opponent) Some(state.opponent) else None
      )
      state.copy(events = enriched_event :: state.events)
    }

    (raw_events.foldLeft(PossState(0, 0, Nil, Direction.Init)) {
      case (state, ev @ LineupEvent.RawGameEvent.Opponent(opp_info)) if is_ignorable_event(opp_info) =>
        enrich(state, ev) //(ignore sub data or selected game data - see above)
      case (state, ev @ LineupEvent.RawGameEvent.Team(team_info)) if is_ignorable_game_event(team_info) =>
        enrich(state, ev) //(ignore selected game data - see above)
      case (state @ PossState(_, _, _, Direction.Init), ev @ LineupEvent.RawGameEvent.Opponent(_)) =>
        enrich(state.copy(opponent = 1, direction = Direction.Opponent), ev)
      case (state @ PossState(_, _, _, Direction.Init), ev @ LineupEvent.RawGameEvent.Team(_)) =>
        enrich(state.copy(team = 1, direction = Direction.Team), ev)
      case (state @ PossState(_, opp_poss, _, Direction.Team), ev @ LineupEvent.RawGameEvent.Opponent(_)) =>
        enrich(state.copy(opponent = opp_poss + 1, direction = Direction.Opponent), ev)
      case (state @ PossState(team_poss, _, _, Direction.Opponent), ev @ LineupEvent.RawGameEvent.Team(_)) =>
        enrich(state.copy(team = team_poss + 1, direction = Direction.Team), ev)
      case (state, ev) =>
        enrich(state, ev) //(all other cases just do nothing)
    }) match {
      case PossState(team, opp, events, _) => (team, opp, events.reverse)
    }
  }
}
object LineupUtils extends LineupUtils
