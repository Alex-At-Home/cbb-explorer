
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

  // Jump ball

  /** We don't care if it was won or lost actually */
  object ParseJumpballWonOrLost {
    // Examples:
    // New:
    //RawGameEvent(None, Some("19:58:00,0-0,Kavell Bigby-Williams, jumpball lost")),
    //RawGameEvent(Some("19:58:00,0-0,Bruno Fernando, jumpball won"), None),
    // Legacy: (none)
    private val jumpball_regex = "[^,]+,[^,]+,(.+), +jumpball (?:won|lost)".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(jumpball_regex(player)) => Some(player)
      case _ => None
    }
  }

  /** Blocked shot (hardwire "Team" as return value)*/
  object ParseTimeout {
    // New:
    //RawGameEvent(None, Some("04:04:00,26-33,Team, timeout short")),
    // Legacy:
    //"team": ""00:21,59-62,TEAM 30 Second Timeout""
    private val timeout_regex = "[^,]+,[^,]+,(.+) +Timeout".r
    private val timeout_regex_new = "[^,]+,[^,]+,(.+), +timeout.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(timeout_regex(_)) => Some("TEAM")
      case Some(timeout_regex_new(_)) => Some("Team")
      case _ => None
    }
  }

  // All the different types of shot

  /** Dunk or "alleyoop" success */
  object ParseDunkMade {
    // New:
    //Bruno Fernando, 2pt dunk 2ndchance;pointsinthepaint made
    //Bruno Fernando, 2pt alleyoop pointsinthepaint made
    // Legacy:
    // WATKINS,MIKE made Dunk

    //TODO
  }

  /** Dunk or "alleyoop" missed */
  object ParseDunkMissed {
    //New:
    //Bruno Fernando, 2pt dunk missed
    // Legacy:
    // WATKINS,MIKE missed Dunk

    //TODO
  }

  /** Layup, success */
  object ParseLayupMade {
    // New:
    // Jalen Smith, 2pt layup 2ndchance;pointsinthepaint made
    // Legacy:
    // BOLTON,RASIR made Layup
    // STEVENS,LAMAR made Tip In

    //TODO
  }
  object ParseLayupMissed {
    // New:
    // Eric Carter, 2pt layup missed
    // Legacy:
    // TOMAIC,JOSHUA missed Layup

    //TODO
  }
  object ParseMidrangeMade {
    // New:
    // Anthony Cowan, 2pt jumpshot fromturnover;fastbreak made
    // Legacy:
    // STEVENS,LAMAR made Two Point Jumper

    //TODO
  }
  object ParseMidrangeMissed {
    // New:
    // Ricky Lindo Jr., 2pt jumpshot missed
    // Legacy:
    // SMITH,JALEN missed Two Point Jumper

    //TODO
  }
  object ParseThreePointerMade {
    // New:
    // Eric Ayala, 3pt jumpshot made
    // Legacy:
    // SMITH,JALEN made Three Point Jumper

    //TODO
  }
  object ParseThreePointerMissed {
    // New:
    // Eric Ayala, 3pt jumpshot 2ndchance missed
    // Legacy:
    // DREAD,MYLES missed Three Point Jumper

    //TODO
  }

  /** Umbrella for all made shots, union of the above */
  object ParseShotMade {
    // New:
    // See above for all the combos, basically "(2pt|3pt) <other stuff> made"
    // Old:
    // See above for all the combos, basically "made {not Free Throw}"

    private val shot_made_regex = "[^,]+,[^,]+,(.+) made +(?!Free Throw).*".r
    private val shot_made_regex_new = "[^,]+,[^,]+,(.+), +[23]pt +(?:.* +)?made".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(shot_made_regex(player)) => Some(player)
      case Some(shot_made_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Umbrella for all made shots, union of the above */
  object ParseShotMissed {
    // New:
    // See above for all the combos, basically "(2pt|3pt) <other stuff> (missed|blocked)""
    // Old:
    // See above for all the combos, basically "missed {not Free Throw}"

    private val shot_missed_regex = "[^,]+,[^,]+,(.+) missed +(?!Free Throw).*".r
    private val shot_missed_regex_new = "[^,]+,[^,]+,(.+), +[23]pt +(?:.* +)?(?:missed|blocked)".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(shot_missed_regex(player)) => Some(player)
      case Some(shot_missed_regex_new(player)) => Some(player)
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
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(blocked_shot_regex(player)) => Some(player)
      case Some(blocked_shot_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  // Rebounding (can tell ORB vs DRB based on possession)

  object ParseRebound {
    // New:
    // Darryl Morsell, rebound defensive
    // Jalen Smith, rebound offensive
    // Team, rebound offensive team
    // Legacy:
    // SMITH,JALEN Offensive Rebound
    // HARRAR,JOHN Defensive Rebound

    private val rebound_regex = "[^,]+,[^,]+,(.+) +(?:Offensive|Defensive) +Rebound".r
    private val rebound_regex_new = "[^,]+,[^,]+,(.+), +rebound +.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(rebound_regex(player)) => Some(player)
      case Some(rebound_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  //TODO: categorize ORB vs DRB for easier stats collection

  // Free throws

  object ParseFreeThrowMade {
    //New: (warning .. free throws can come in the wrong order)
    // Kevin Anderson, freethrow 2of2 made
    //Legacy:
    //DREAD,MYLES made Free Throw
    private val ft_made_regex = "[^,]+,[^,]+,(.+) made +Free Throw".r
    private val ft_made_regex_new = "[^,]+,[^,]+,(.+), +freethrow [0-9]of[0-9] +(?:.* +)?made".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(ft_made_regex(player)) => Some(player)
      case Some(ft_made_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  object ParseFreeThrowMissed {
    //New: (warning .. free throws can come in the wrong order)
    // Kevin Anderson, freethrow 1of2 missed
    //Legacy:
    //DREAD,MYLES missed Free Throw
    private val ft_missed_regex = "[^,]+,[^,]+,(.+) missed +Free Throw".r
    private val ft_missed_regex_new = "[^,]+,[^,]+,(.+), +freethrow [0-9]of[0-9] +(?:.* +)?missed".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(ft_missed_regex(player)) => Some(player)
      case Some(ft_missed_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  // Turnover events

  object ParseTurnover {
      // New:
      // Bruno Fernando, turnover badpass
      // Joshua Tomaic, turnover lostball
      // Jalen Smith, turnover offensive
      // Kevin Anderson, turnover travel
      // Legacy:
      // MORSELL,DARRYL Turnover

      private val turnover_regex = "[^,]+,[^,]+,(.+) +Turnover".r
      private val turnover_regex_new = "[^,]+,[^,]+,(.+), +turnover +.*".r
      def unapply(x: String): Option[String] = Option(x) match {
        case Some(turnover_regex(player)) => Some(player)
        case Some(turnover_regex_new(player)) => Some(player)
        case _ => None
      }
  }

  /** Steal */
  object ParseStolen {
    // New:
    //RawGameEvent(None, Some("08:44:00,20-23,Jacob Cushing, steal")),
    // Legacy:
    //"team": "05:10,55-68,MASON III,FRANK Steal"
    private val stolen_regex = "[^,]+,[^,]+,(.+) +Steal".r
    private val stolen_regex_new = "[^,]+,[^,]+,(.+), +steal".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(stolen_regex(player)) => Some(player)
      case Some(stolen_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  // Foul events

  /** Personal foul */
  object ParsePersonalFoul {
    // New:
    //RawGameEvent(Some("13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow"), None)
    // Legacy:
    //"opponent": "10:00,51-60,MYKHAILIUK,SVI Commits Foul"
    private val personal_foul_regex = "[^,]+,[^,]+,(.+) +Commits Foul".r
    private val personal_foul_regex_new = "[^,]+,[^,]+,(.+), +foul personal.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(personal_foul_regex(player)) => Some(player)
      case Some(personal_foul_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Technical foul */
  object ParseTechnicalFoul {
    // New:
    //"team": "06:43:00,55-79,Bruno Fernando, foul technical classa;2freethrow"
    // Legacy:
    //(haven't found any yet)
    private val technical_foul_regex_new = "[^,]+,[^,]+,(.+), +foul technical.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(technical_foul_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Who was fouled? */
  object ParseFoulInfo {
    // New:
    //RawGameEvent(None, Some(02:28:00,27-38,Jalen Smith, foulon), None, Some(1))
    // Legacy:
    //(haven't found any yet)
    private val foul_info_regex_new = "[^,]+,[^,]+,(.+), +foulon".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(foul_info_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  // Useful

  /** An offensive event that tells us who is starting a possession */
  object ParseCommonOffensiveEvent {
    def unapply(x: String): Option[String] = x match {
      case ParseTimeout(x) => Some(x)
      case ParseShotMade(x) => Some(x)
      case ParseShotMissed(x) => Some(x)
      case ParseTurnover(x) => Some(x)
    }
  }

  /** An offensive event that tells us who is starting a possession */
  object ParseCommonDefensiveEvent {
    def unapply(x: String): Option[String] = x match {
      case ParsePersonalFoul(x) => Some(x)
      case ParseStolen(x) => Some(x)
      case ParseShotBlocked(x) => Some(x)
    }
  }

}
