package org.piggottfamily.cbb_explorer.models

/**
 * Contains information about a team's season
 * @param XXX TODO
 */
case class TeamSeason(
	team_season: TeamSeasonId,
	stats: TeamSeasonStats,
	games: List[Game],
	players: Map[PlayerId, PlayerSeason],
	coach: CoachId
) {
	// Games views
	//TODO: do I want these, or just "games.filter(_.is_conf)"
	// def conf_games: List[Game] = ???
	// def nonconf_games: List[Game] = ???
	// def nit_games: List[Game] = ???
	// def ncaa_games: List[Game] = ???
	// def homes_games: List[Game] = ???
	// def away_games: List[Game] = ???
	// def neutral_games: List[Game] = ???
	// def won_games: List[Game] = ???
	// def lost_games: List[Game] = ???
}
