package org.piggottfamily.cbb_explorer.models.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa.LineupEvent._
import org.joda.time.DateTime

/** Info about each shot taken during a game. All distances are in feet
  */
final case class ShotEvent(
    shooter: Option[LineupEvent.PlayerCodeId],
    date: DateTime,
    location_type: Game.LocationType.Value,
    team: TeamSeasonId,
    opponent: TeamSeasonId,
    is_off: Boolean,
    lineup_id: LineupEvent.LineupId,
    players: List[LineupEvent.PlayerCodeId],
    score: Game.Score,
    shot_min: Double,
    x: Double,
    y: Double,
    dist: Double,
    pts: Int,
    value: Int,
    assisted_by: Option[LineupEvent.PlayerCodeId],
    is_assisted: Option[Boolean],
    in_transition: Option[Boolean]
)
