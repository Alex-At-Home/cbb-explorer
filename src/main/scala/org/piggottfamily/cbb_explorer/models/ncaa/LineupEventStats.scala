package org.piggottfamily.cbb_explorer.models.ncaa

/**
 * Contains a collection of counting stats
 */
case class LineupEventStats(
  num_events: Int = 0,
  num_possessions: Int = 0,

  fg: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_rim: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_2p: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_3p: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  ft: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),

  orb: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  drb: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),

  to: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  stl: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  assists: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),

  fouls: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),

  pts: Int = 0,
  plus_minus: Int = 0
)

object LineupEventStats {
  val empty = LineupEventStats()

  /**
   * Break down of counting stats specific to shot clock + non-shot events
   * @param total Count across the entire shot clock
   * @param early In the first 10s
   * @param mid In the middle 10s
   * @param late In the last 10s
   * @param orb In the first 10s following an ORB (else counts as mid/late as normal)
   */
  case class ShotClockStats(
    total: Int = 0,
    early: Int = 0,
    mid: Int = 0,
    late: Int = 0,
    orb: Int = 0
  )

  /**
   * Break down of counting stats specific to field goals/shot clock
   * @param attempts The number of shot attempts, successful or not
   * @param made The number of successful shot attempts
   * @param pct A rounded integer with the %
   * @param pct A rounded integer with the effective % given the shot type
   */
  case class FieldGoalStats(
    attempts: ShotClockStats = ShotClockStats(),
    made: ShotClockStats = ShotClockStats(),
    pct: ShotClockStats = ShotClockStats(),
    effective_pct: ShotClockStats = ShotClockStats()
  )

}
