package org.piggottfamily.cbb_explorer.models.play_types

import org.piggottfamily.cbb_explorer.models._
import org.joda.time.DateTime

/**
 * Represents an event or slice of events for which there is a play type breakdown
 */
case class PlayTypeEvent(
  date: DateTime,
  location_type: Game.LocationType.Value,
  score_info: LineupEvent.ScoreInfo,
  team: TeamSeasonId,
  opponent: TeamSeasonId,
  start_min: Double,
  end_min: Double,
  transition: Option[PlayTypeEvent.Stats],
  pnr_bh_shot: Option[PlayTypeEvent.Stats],
  pnr_bh_pass: Option[PlayTypeEvent.Stats],
  pnr_bh_roll: Option[PlayTypeEvent.Stats],
  postup_shot: Option[PlayTypeEvent.Stats],
  postup_pass: Option[PlayTypeEvent.Stats],
  off_ball: Option[PlayTypeEvent.Stats],
  misc_cut: Option[PlayTypeEvent.Stats],
  misc_spotup: Option[PlayTypeEvent.Stats],
  iso: Option[PlayTypeEvent.Stats],
  putback: Option[PlayTypeEvent.Stats],
  uncategorized_plays: Int,
  recycle_orb: Int
)

object PlayTypeEvent {

  /** Which team is in possession */
  object PlayTypeCategory extends Enumeration {
    val transition, pnr_bh_shot, pnr_bh_pass,
        pnr_bh_roll, postup_shot, postup_pass, off_ball, misc_cut,
        misc_spotup, iso, putback = Value
  }

  /** The per possession stats */
  case class Stats(
    category: PlayTypeEvent.PlayTypeCategory.Value,
    num_plays: Int,
    num_pts: Int,
    _3pa: Int,
    _3pm: Int,
    _2pa: Int,
    _2pm: Int,
    quality: Int,
    _to: Int,
    sf: Int,
    orb: Int,
    game_events: List[String]
  )
}
