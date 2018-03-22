package org.piggottfamily.cbb_explorer.models

/**
 * Contains statistical information about a team's season
 * @param XXX TODO
 */
case class TeamSeasonStats( //TODO conf-only and season-wide
	//TODO: results, or put that somewhere else
	//Overall
	adj_margin: Metric,
	adj_off: Metric,
	adj_def: Metric
	//Components etc etc
)
//TODO: how to handle different sources
