package org.piggottfamily.cbb_explorer.models

/**
 * Contains top-level information about a player's statistical season
 * @param XXX TODO
 */
case class PlayerSeasonSummaryStats(
    player_class: PlayerClass,
    off_eff: Metric,
    shot_pct: Metric,
    minutes_pct: Metric
)
