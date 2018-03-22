package org.piggottfamily.cbb_explorer.models

/**
 * The value for each advanced metric, together with its ordering
 * @param value The score for that metric
 * @param rank Where this metric ranks against all other metrics that season for this context (eg national or conference)
 */
case class Metric(value: Double, rank: Int) //TODO can this be AnyVal

//TODO: things like adj_off have conference value so this doesn't work
