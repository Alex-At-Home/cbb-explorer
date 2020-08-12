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
  ast_rim: LineupEventStats.AssistInfo = LineupEventStats.AssistInfo(),
  ast_mid: LineupEventStats.AssistInfo = LineupEventStats.AssistInfo(),
  ast_3p: LineupEventStats.AssistInfo = LineupEventStats.AssistInfo(),

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
   * A list of all the assists from or to players
   * @param player_code - the other player in the assist event (by code not name)
   * @param count - the count of assists (by shot clock, like everything else)
   */
  case class AssistEvent(
    player_code: String,
    count: ShotClockStats = ShotClockStats()
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

  /**
   * More detailed assist info
   * @param counts - raw statistics
   * @param target - a list of players who "I" assisted
   * @param source - a list of players who assisted "me"
   */
  case class AssistInfo(
    counts: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
    target: List[AssistEvent] = Nil,
    source: List[AssistEvent] = Nil
  )

}
