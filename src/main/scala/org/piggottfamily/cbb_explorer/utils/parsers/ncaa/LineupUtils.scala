package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.utils.StateUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

import com.softwaremill.quicklens._

/** Utilities related to building up the lineup stats */
trait LineupUtils {
  import ExtractorUtils._

  /** TODO build the stats from the game events */
  def enrich_lineup(lineup: LineupEvent): LineupEvent =
  {
    val scored = lineup.score_info.end.scored - lineup.score_info.start.scored
    val allowed = lineup.score_info.end.allowed - lineup.score_info.start.allowed

    lineup.copy(
      team_stats = lineup.team_stats.copy(
        pts = scored,
        plus_minus = scored - allowed
      ),
      opponent_stats = lineup.opponent_stats.copy(
        pts = allowed,
        plus_minus = allowed - scored
      )
    )
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

  /** Takes a unfiltered set of game events
   *  builds all the counting stats
   * TODO: figure out start of possession times and use
   */
  protected def enrich_stats(
    evs: List[LineupEvent.RawGameEvent],
    event_parser: LineupEvent.RawGameEvent.PossessionEvent,
    player_filter: Option[String] = None
  ): LineupEventStats => LineupEventStats = { case stats: LineupEventStats =>
      case class StatsBuilder(curr: LineupEventStats)

      //TODO: build some lenses
      val selector_shotclock_total = modify[LineupEventStats.ShotClockStats](_.total)
      def shotclock_selectors() = List(selector_shotclock_total) // TODO
      def increment_misc_stat(
        selector: PathLazyModify[StatsBuilder, LineupEventStats.ShotClockStats], state: StatsBuilder
      ): StatsBuilder  =
        shotclock_selectors().foldLeft(state) { (acc, shotclock_selector) =>
          (selector andThenModify shotclock_selector).using(_ + 1)(acc)
        }

      val starting_state = StatsBuilder(stats)
      (evs.foldLeft(starting_state) {

        // Field goal stats

        // Misc stats

        case (state, event_parser.AttackingTeam(ev_str @ EventUtils.ParseOffensiveRebound(player)))
          if player_filter.forall(_ == player)
          /** TODO: need to ignore actual deadball rebounds..., for now just discard? */
            && EventUtils.ParseOffensiveDeadballRebound.unapply(ev_str).isEmpty
        =>
          increment_misc_stat(modify[StatsBuilder](_.curr.orb), state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseDefensiveRebound(player)))
          if player_filter.forall(_ == player)
        =>
          increment_misc_stat(modify[StatsBuilder](_.curr.drb), state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseTurnover(player)))
          if player_filter.forall(_ == player)
        =>
          increment_misc_stat(modify[StatsBuilder](_.curr.to), state)

        case (state, event_parser.AttackingTeam(EventUtils.ParseStolen(player)))
          if player_filter.forall(_ == player)
        =>
          increment_misc_stat(modify[StatsBuilder](_.curr.stl), state)

/**TODO: parse assist*/
        // case (state, event_parser.AttackingTeam(EventUtils.ParseAssist(player)))
        //   if player_filter.forall(_ == player)
        // =>
        //   increment_misc_stat(modify[StatsBuilder](_.curr.assist), state)

        case (state, event_parser.AttackingTeam(EventUtils.ParsePersonalFoul(player)))
          if player_filter.forall(_ == player)
        =>
          increment_misc_stat(modify[StatsBuilder](_.curr.foul), state)

        case (state, _) => state
      }).curr
    }
//TODO: TOTEST

  // def add_stats_to_lineups(lineup: LineupEvent): LineupEvent = {
  //
  // }

}
object LineupUtils extends LineupUtils
