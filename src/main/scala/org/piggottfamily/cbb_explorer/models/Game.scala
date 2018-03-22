package org.piggottfamily.cbb_explorer.models

/**
 * Contains information about a game from the point of view of one of the teams
 * @param XXX TODO
 */
case class Game(
  opponent: TeamSeasonId,
  won: Boolean,
  rank: Int,
  opp_rank: Int,
  location_type: Game.LocationType.Value,
  tier: Game.PrimaryTier.Value,
  secondary_tiers: Set[Game.SecondaryTier.Value]
) {
}

object Game {

  /** Location of game (semi-home is neutral site with clear advantage etc) */
  object LocationType extends Enumeration {
    val Home, Away, Neutral, SemiHome, SemiAway = Value
  }

  /** KenPom tiers */
  object PrimaryTier extends Enumeration {
    val A, B, C, D = Value
  }
  /** My additional tiers */
  object SecondaryTier extends Enumeration {
    val A_star = Value
  }
}
