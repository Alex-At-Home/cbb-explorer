package org.piggottfamily.cbb_explorer.models

/**
 * The value for each advanced metric, together with its ordering
 * @param value The score for that metric
 * @param rank Where this metric ranks against all other metrics that season for this context (eg national or conference)
 */
case class Metric(value: Double, rank: Int) //TODO can this be AnyVal

object Metric {
  /** constant in value field meaning that value could not be derived */
  val no_value = -1.0
  /** constant in rank field meaning that rank could not be derived */
  val no_rank = -1
  /** The metric singleton represening a missing/dummy metric */
  val empty = Metric(no_value, no_rank)
  /** Metric is dummy value */
  def is_empty(m: Metric): Boolean = m == empty
  /** Metric has no rank */
  def has_rank(m: Metric): Boolean = m.rank != no_rank
}
