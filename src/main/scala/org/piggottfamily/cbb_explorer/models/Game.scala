package org.piggottfamily.cbb_explorer.models

import org.joda.time.DateTime

/**
 * Contains information about a game from the point of view of one of the teams
 * @param XXX TODO
 */
case class Game(
  opponent: TeamSeasonId,
  date: DateTime,
  won: Boolean,
  score: Game.Score,
  pace: Int,
  rank: Int,
  opp_rank: Int,
  location_type: Game.LocationType.Value,
  tier: Game.TierType.Value
)

object Game {

  /** Encapsulates the score */
  case class Score(scored: Int, allowed: Int)

  /** Some score utilities */
  object Score {
    def by_winner(g: Game): Score = {
      if (g.won) g.score else Score(g.score.allowed, g.score.scored)
    }
    def by_location(g: Game): Score = g.location_type match {
      case LocationType.Home | LocationType.SemiHome | LocationType.Neutral => g.score
      case _ => Score(g.score.allowed, g.score.scored)
    }
  }

  /** Location of game (semi-home is neutral site with clear advantage etc) */
  object LocationType extends Enumeration {
    val Home, Away, Neutral, SemiHome, SemiAway = Value
  }

  /** KenPom tiers */
  object TierType extends Enumeration {
    val A, B, C, D = Value
  }
}
