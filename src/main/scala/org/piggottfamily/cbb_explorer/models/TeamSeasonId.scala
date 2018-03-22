package org.piggottfamily.cbb_explorer.models

/** 
 * Represents a an identifier for a season played by a CBB team
 * @param team The team playing the season
 * @param year The year the season ends
 */
case class TeamSeasonId(team: TeamId, year: Year) 
