package org.piggottfamily.cbb_explorer.models.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa.LineupEvent._
import org.joda.time.DateTime

/** Info about each shot taken during a game. All distances are in feet
  */
final case class ShotEvent(
    player: Option[LineupEvent.PlayerCodeId],
    date: DateTime,
    location_type: Game.LocationType.Value,
    team: TeamSeasonId,
    opponent: TeamSeasonId,
    is_off: Boolean,
    lineup_id: Option[LineupEvent.LineupId], // (discard if bad lineup)
    players: List[LineupEvent.PlayerCodeId],
    score: Game.Score,
    min: Double,
    loc: ShotEvent.ShotLocation,
    geo: ShotEvent.ShotGeo,
    dist: Double,
    pts: Int,
    value: Int,
    ast_by: Option[LineupEvent.PlayerCodeId],
    is_ast: Option[Boolean],
    is_trans: Option[Boolean],
    raw_event: Option[String] // (discard before writing to disk)
)

final case class CutdownShotEvent(
    loc: Option[ShotEvent.ShotLocation],
    geo: Option[ShotEvent.ShotGeo],
    dist: Option[Double],
    pts: Int,
    value: Int,
    is_ast: Option[Boolean],
    is_trans: Option[Boolean],
    is_orb: Option[Boolean]
)

object ShotEvent {

  /** Will turn ft into meters to make the granularity of the data better */
  case class ShotGeo(lat: Double, lon: Double)

  /** In ft: >0 is right of the basket, <0 is left */
  case class ShotLocation(x: Double, y: Double)
}
