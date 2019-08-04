
package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

/** Utilities related to converting strings into structure events */
object EventUtils {

  /////////////////////////////////////////////////

  // Date-time parser

  /** Gets the game time from the raw event */
  object ParseGameTime {
    private val time_regex = "([0-9]+):([0-9]+)(?:[:]([0-9]+))?".r
    def unapply(x: String): Option[Double] = x match {
      case time_regex(min, secs, maybe_csecs) => Some(
        min.toInt*1.0 + secs.toInt/60.0
          + Option(maybe_csecs).map(_.toInt).getOrElse(0)/6000.0
      )
      case _ => None
    }
  }

  /////////////////////////////////////////////////

  // Opponent substitution events (NOTE: only on the event, not on the score or event string)
  // (this is also used during the construction of the play-by-play data, unlike other events)

  /** Team (_not_ opponent) substitution in */
  object ParseTeamSubIn {
    private val sub_regex_in = "(.+) +Enters Game".r
    private val sub_regex_in_new_format = "(.+), +substitution in".r
    def unapply(x: (Option[String], Option[String])): Option[String] = x match {
      case (Some(sub_regex_in(player)), None) => Some(player)
      case (Some(sub_regex_in_new_format(player)), None) => Some(player)
      case _ => None
    }
  }
  /** Team (_not_ opponent) substitution out */
  object ParseTeamSubOut {
    private val sub_regex_out = "(.+) +Leaves Game".r
    private val sub_regex_in_new_format = "(.+), +substitution out".r
    def unapply(x: (Option[String], Option[String])): Option[String] = x match {
      case (Some(sub_regex_out(player)), None) => Some(player)
      case (Some(sub_regex_in_new_format(player)), None) => Some(player)
      case _ => None
    }
  }

  /////////////////////////////////////////////////

  // In-game events (NOTE: based on the combined time,score,event)

  //TODO: always pull out the times here?

  // Jump ball

  /** We don't care if it was won or lost actually */
  object ParseJumpballWonOrLost {
    // Examples:
    // New:
    //RawGameEvent(None, Some("19:58:00,0-0,Kavell Bigby-Williams, jumpball lost")),
    //RawGameEvent(Some("19:58:00,0-0,Bruno Fernando, jumpball won"), None),
    // Legacy: (none)
    private val jumpball_regex = "[^,]+,[^,]+,(.+), +jumpball (?:won|lost)".r
//    private val jumpball_regex = "(.+), +jumpball (won|lost)".r
    def unapply(x: Option[String]): Option[String] = x match {
      case Some(jumpball_regex(player)) => Some(player)
      case _ => None
    }
  }

  /** Blocked shot */
  object ParseShotBlocked {
    // New:
    //RawGameEvent(None, Some("14:11:00,7-9,Emmitt Williams, block")),
    // Legacy:
    //"team": "04:53,55-69,LAYMAN,JAKE Blocked Shot"
    private val blocked_shot_regex = "[^,]+,[^,]+,(.+) +Blocked Shot".r
    private val blocked_shot_regex_new = "[^,]+,[^,]+,(.+), +block".r
    def unapply(x: Option[String]): Option[String] = x match {
      case Some(blocked_shot_regex(player)) => Some(player)
      case Some(blocked_shot_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Personal foul */
  object ParsePersonalFoul {
    // New:
    //RawGameEvent(Some("13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow"), None)
    // Legacy:
    //"opponent": "10:00,51-60,MYKHAILIUK,SVI Commits Foul"
    private val personal_foul_regex = "[^,]+,[^,]+,(.+) +Commits Foul".r
    private val personal_foul_regex_new = "[^,]+,[^,]+,(.+), +foul personal.*".r
    def unapply(x: Option[String]): Option[String] = x match {
      case Some(personal_foul_regex(player)) => Some(player)
      case Some(personal_foul_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Personal foul */
  object ParseTechnicalFoul {
    // New:
    //"team": "06:43:00,55-79,Bruno Fernando, foul technical classa;2freethrow"
    // Legacy:
    //(haven't found any yet)
    private val technical_foul_regex_new = "[^,]+,[^,]+,(.+), +foul technical.*".r
    def unapply(x: Option[String]): Option[String] = x match {
      case Some(technical_foul_regex_new(player)) => Some(player)
      case _ => None
    }
  }
}
