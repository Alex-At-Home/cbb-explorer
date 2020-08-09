package org.piggottfamily.cbb_explorer.models.ncaa

/**
 * Contains a collection of counting stats
 */
case class LineupEventStats(
  num_events: Int = 0,
  num_possessions: Int = 0,

  fg: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_rim: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_mid: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_2p: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_3p: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  ft: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),

  orb: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  drb: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),

  to: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  stl: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  blk: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),

  assist: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  assist_rim: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  assist_mid: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  assist_3p: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),

  foul: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),

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
   * @param ast The number of successful shot attempts that were assisted
   */
  case class FieldGoalStats(
    attempts: ShotClockStats = ShotClockStats(),
    made: ShotClockStats = ShotClockStats(),
    ast: ShotClockStats = ShotClockStats(),
  )

}
