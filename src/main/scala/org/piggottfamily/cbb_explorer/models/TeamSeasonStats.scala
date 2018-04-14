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
	adj_def: Metric,
	adj_tempo: Metric,
	off: TeamSeasonStats.OffenseDefenseStats,
	_def: TeamSeasonStats.OffenseDefenseStats
	//TODO SoS, personnel etc
)
//TODO: how to handle different sources

object TeamSeasonStats {
	case class OffenseDefenseStats(
		avg_poss_len: Metric,
		eff_fg: Metric,
		to_pct: Metric,
		orb_pct: Metric,
		ft_rate: Metric,
		_3p_pct: Metric,
		_2p_pct: Metric,
		ft_pct: Metric,
		blk_pct: Metric,
		stl_pct: Metric,
		_3pa_rate: Metric,
		afgm_rate: Metric,
		_3p_pt_dist: Metric,
		_2p_pt_dist: Metric,
		ft_pt_dist: Metric
	)
}
