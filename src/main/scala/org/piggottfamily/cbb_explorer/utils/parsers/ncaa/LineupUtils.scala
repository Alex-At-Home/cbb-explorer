package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

/** Utilities related to building up the lineup stats */
trait LineupUtils {
  import ExtractorUtils._

  /** TODO build the stats from the game events */
  def enrich_lineup(lineup: LineupEvent, prev_lineup: Option[LineupEvent])
    : (LineupEvent, Option[LineupEvent]) =
  {
    val scored = lineup.score_info.end.scored - lineup.score_info.start.scored
    val allowed = lineup.score_info.end.allowed - lineup.score_info.start.allowed
    val last_game_event =  prev_lineup.flatMap(_.raw_game_events.lastOption)
    val (team_possessions, opp_possessions, proc_events, adjust_prev_lineup) =
      calculate_possessions(lineup.raw_game_events, last_game_event)

    val adjusted_prev_lineup = prev_lineup.map { prev => (last_game_event, adjust_prev_lineup) match {
      case (Some(last_ev), true) if last_ev.team_possession.nonEmpty =>
        prev.copy(
          team_stats = prev.team_stats.copy(
            num_possessions = prev.team_stats.num_possessions - 1
          )
        )
      case (Some(last_ev), true) if last_ev.opponent_possession.nonEmpty =>
        prev.copy(
          opponent_stats = prev.opponent_stats.copy(
            num_possessions = prev.opponent_stats.num_possessions - 1
          )
        )
      case _ => prev
    }}

    lineup.copy(
      team_stats = lineup.team_stats.copy(
        num_events = lineup.raw_game_events.filter(_.team.isDefined).size,
        num_possessions = team_possessions //TODO
        + adjusted_prev_lineup.map(_.team_stats.num_possessions).getOrElse(0)
        ,
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
    ) -> adjusted_prev_lineup
  }

  /** There is a weird bug that has happened one time where the scores got swapped */
  def fix_possible_score_swap_bug(lineup: List[LineupEvent], box_lineup: LineupEvent)
    : List[LineupEvent] =
  {
    def scores_reversed(g1: Game.Score, g2: Game.Score): Boolean = {
      (g1.scored == g2.allowed) && (g2.scored == g1.allowed)
    }
    lineup.lastOption.map(_.score_info) match {
      case Some(LineupEvent.ScoreInfo(_, final_score, _, _))
        if scores_reversed(final_score, box_lineup.score_info.end)
      =>
        def reverse_score(score: Game.Score): Game.Score =
          score.copy(allowed = score.scored, scored = score.allowed)
        def reverse_scores(score_info: LineupEvent.ScoreInfo): LineupEvent.ScoreInfo = {
          score_info.copy(
            start = reverse_score(score_info.start),
            end = reverse_score(score_info.end),
            start_diff = -score_info.start_diff,
            end_diff = -score_info.end_diff
          )
        }
        lineup.map { x =>
          val t_pts = x.team_stats.pts
          val o_pts = x.opponent_stats.pts
          x.copy(
            score_info = reverse_scores(x.score_info),
            team_stats = x.team_stats.copy(
              pts = o_pts,
              plus_minus = -x.team_stats.plus_minus
            ),
            opponent_stats = x.opponent_stats.copy(
              pts = t_pts,
              plus_minus = -x.opponent_stats.plus_minus
            )
          )
        }
      case _ => lineup //(we're good, nothing to do)
    }
  }

  /** Returns team and opponent possessions based on the raw event data */
  protected def calculate_possessions(
    raw_events: Seq[LineupEvent.RawGameEvent], last_event: Option[LineupEvent.RawGameEvent]
  ): (Int, Int, List[LineupEvent.RawGameEvent], Boolean) =
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
      // 1.1] Jump ball
      case EventUtils.ParseJumpballWonOrLost(_) => true

      // 1.2] Timeout
      case EventUtils.ParseTimeout(_) => true

      // 2.1] Blocks (wait for rebound to decide if the possession changes)
      // New:
      //RawGameEvent(Some("14:11:00,7-9,Darryl Morsell, 2pt layup blocked missed"), None),
      //RawGameEvent(None, Some("14:11:00,7-9,Emmitt Williams, block")),
      //RawGameEvent(Some("14:11:00,7-9,Team, rebound offensivedeadball"), None),
      // Legacy:
      //"team": "04:53,55-69,LAYMAN,JAKE Blocked Shot"
      //"opponent": "04:53,55-69,TEAM Offensive Rebound"
      case EventUtils.ParseShotBlocked(_) => true

      // 2.2] Steals - possession always changes but we'll wait for the offensive action
      case EventUtils.ParseStolen(_) => true

      // 3.1] Personal Fouls
      // New:
      //RawGameEvent(None, Some("13:36:00,7-9,Team, rebound offensivedeadball")),
      //RawGameEvent(Some("13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow"), None)
      // Legacy:
      //"opponent": "10:00,51-60,MYKHAILIUK,SVI Commits Foul"
      case EventUtils.ParsePersonalFoul(_) => true

      // 3.2] Technical fouls
      // We're going to treat the technical foul like an additional possession
      // because otherwise it's going to be complicated
      case EventUtils.ParseTechnicalFoul(_) => true

      // 3.3] Foul info
      // Contains no possession related value
      case EventUtils.ParseFoulInfo(_) => true

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
      direction: Direction.Value,
      adjust_prev_lineup: Boolean
    )

    /** Adds the possession count to the event */
    def enrich(state: PossState, ev: LineupEvent.RawGameEvent): PossState = {
      val enriched_event = ev.copy(
        team_possession = if (state.direction == Direction.Team) Some(state.team) else None,
        opponent_possession = if (state.direction == Direction.Opponent) Some(state.opponent) else None
      )
      state.copy(events = enriched_event :: state.events)
    }
    /** First non-ignorable event ... are we "stealing" the last lineup's final possession */
    def check_for_prev_lineup_adjustment(
      ev: LineupEvent.RawGameEvent, prev_event: Option[LineupEvent.RawGameEvent]
    ): Boolean = (ev, prev_event) match {
      case (LineupEvent.RawGameEvent.Team(_), Some(prev)) if prev.team_possession.nonEmpty =>
        true
      case (LineupEvent.RawGameEvent.Opponent(_), Some(prev)) if prev.opponent_possession.nonEmpty =>
        true
      case _ => false
    }

    (raw_events.foldLeft(PossState(0, 0, Nil, Direction.Init, false)) {
      case (state, ev @ LineupEvent.RawGameEvent.Opponent(opp_info)) if is_ignorable_event(opp_info) =>
        enrich(state, ev) //(ignore sub data or selected game data - see above)
      case (state, ev @ LineupEvent.RawGameEvent.Team(team_info)) if is_ignorable_game_event(team_info) =>
        enrich(state, ev) //(ignore selected game data - see above)

      case (state @ PossState(_, _, _, Direction.Init, _), ev @ LineupEvent.RawGameEvent.Opponent(_)) =>
        enrich(state.copy(
          opponent = 1, direction = Direction.Opponent,
          adjust_prev_lineup = check_for_prev_lineup_adjustment(ev, last_event)
        ), ev)
      case (state @ PossState(_, _, _, Direction.Init, _), ev @ LineupEvent.RawGameEvent.Team(_)) =>
        enrich(state.copy(
          team = 1, direction = Direction.Team,
          adjust_prev_lineup = check_for_prev_lineup_adjustment(ev, last_event)
        ), ev)

      case (state @ PossState(_, opp_poss, _, Direction.Team, _), ev @ LineupEvent.RawGameEvent.Opponent(_)) =>
        enrich(state.copy(opponent = opp_poss + 1, direction = Direction.Opponent), ev)
      case (state @ PossState(team_poss, _, _, Direction.Opponent, _), ev @ LineupEvent.RawGameEvent.Team(_)) =>
        enrich(state.copy(team = team_poss + 1, direction = Direction.Team), ev)
      case (state, ev) =>
        enrich(state, ev) //(all other cases just do nothing)
    }) match {
      case PossState(team, opp, events, _, adjust_prev_lineup) =>
        (team, opp, events.reverse, adjust_prev_lineup)
    }
  }
}
object LineupUtils extends LineupUtils
