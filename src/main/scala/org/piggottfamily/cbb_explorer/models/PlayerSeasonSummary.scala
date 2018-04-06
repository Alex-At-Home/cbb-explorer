package org.piggottfamily.cbb_explorer.models

/**
 * Contains information about a player's season
 * @param XXX TODO
 */
case class PlayerSeasonSummary(
    id: PlayerId,
    team_season: TeamSeasonId,
    conf: ConferenceId,
    stats: PlayerSeasonSummaryStats
)
