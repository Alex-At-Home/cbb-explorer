package org.piggottfamily.cbb_explorer.models.ncaa

/**
 * Contains a collection of counting stats
 * (player_shot_info - for each spot in the lineup, some minimal info about the sort of shots they took, for upstream luck calcs)
 */
case class LineupEventStats(
  num_events: Int = 0,
  num_possessions: Int = 0,

  // Leave: fg, fg_3p, fg_2p, ft, to as non-optional because they are used commonly

  fg: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_rim: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_mid: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_2p: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  fg_3p: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),
  ft: LineupEventStats.FieldGoalStats = LineupEventStats.FieldGoalStats(),

  orb: Option[LineupEventStats.ShotClockStats] = None,
  drb: Option[LineupEventStats.ShotClockStats] = None,

  to: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
  stl: Option[LineupEventStats.ShotClockStats] = None,
  blk: Option[LineupEventStats.ShotClockStats] = None,

  assist: Option[LineupEventStats.ShotClockStats] = None,
  ast_rim: Option[LineupEventStats.AssistInfo] = None,
  ast_mid: Option[LineupEventStats.AssistInfo] = None,
  ast_3p: Option[LineupEventStats.AssistInfo] = None,

  foul: Option[LineupEventStats.ShotClockStats] = None,

  player_shot_info: Option[LineupEventStats.PlayerShotInfo] = None,

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
    early: Option[Int] = None,
    mid: Option[Int] = None,
    late: Option[Int] = None,
    orb: Option[Int] = None
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
    ast: Option[ShotClockStats] = None,
  )

  /**
   * More detailed assist info
   * @param counts - raw statistics
   * @param target - a list of players who "I" assisted
   * @param source - a list of players who assisted "me"
   */
  case class AssistInfo(
    counts: LineupEventStats.ShotClockStats = LineupEventStats.ShotClockStats(),
    target: Option[List[AssistEvent]] = None,
    source: Option[List[AssistEvent]] = None
  )

  type PlayerTuple[T] = Tuple5[T, T, T, T, T]

  /** Gives each player some info about their stats 
   * The tuples will get serialized into an 8B Long, 12b each in ascending order
  */
  case class PlayerShotInfo(
    unknown_3pM: Option[PlayerTuple[Int]] = None,
    early_3pa: Option[PlayerTuple[Int]] = None,
    unast_3pm: Option[PlayerTuple[Int]] = None,
    ast_3pm: Option[PlayerTuple[Int]] = None
  )
}
