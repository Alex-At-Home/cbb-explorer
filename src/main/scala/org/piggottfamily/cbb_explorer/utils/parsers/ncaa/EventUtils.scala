
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
      case (Some(player), None) => ParseTeamSubIn.unapply(player)
      case _ => None
    }
    def unapply(x: String): Option[String] = x match {
      case sub_regex_in(player) => Some(player)
      case sub_regex_in_new_format(player) => Some(player)
      case _ => None
    }
  }
  /** Team (_not_ opponent) substitution out */
  object ParseTeamSubOut {
    private val sub_regex_out = "(.+) +Leaves Game".r
    private val sub_regex_out_new_format = "(.+), +substitution out".r
    def unapply(x: (Option[String], Option[String])): Option[String] = x match {
      case (Some(player), None) => ParseTeamSubOut.unapply(player)
      case _ => None
    }
    def unapply(x: String): Option[String] = x match {
      case sub_regex_out(player) => Some(player)
      case sub_regex_out_new_format(player) => Some(player)
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
  /** Look for won jumpball */
  object ParseJumpballWon {
    // Examples:
    // New:
    //RawGameEvent(Some("19:58:00,0-0,Bruno Fernando, jumpball won"), None),
    // Legacy: (none)
    private val jumpball_regex = "[^,]+,[^,]+,(.+), +jumpball won".r
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

  /** Dunk or "alleyoop" or layup success */
  object ParseRimMade {
    // New:
    //Bruno Fernando, 2pt dunk 2ndchance;pointsinthepaint made
    //Bruno Fernando, 2pt alleyoop pointsinthepaint made
    // Jalen Smith, 2pt layup 2ndchance;pointsinthepaint made
    // Legacy:
    // WATKINS,MIKE made Dunk
    // BOLTON,RASIR made Layup
    // STEVENS,LAMAR made Tip In

    private val shot_made_regex = "[^,]+,[^,]+,(.+) made +(?:Dunk|Layup|Tip In)".r
    private val shot_made_regex_new = "[^,]+,[^,]+,(.+), +2pt +(?:dunk|layup|alleyoop)(?:.* +)?made".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(shot_made_regex(player)) => Some(player)
      case Some(shot_made_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Dunk or "alleyoop" or layup missed */
  object ParseRimMissed {
    //New:
    //Bruno Fernando, 2pt dunk missed
    // Eric Carter, 2pt layup missed
    // Legacy:
    // WATKINS,MIKE missed Dunk
    // TOMAIC,JOSHUA missed Layup
    // 03:05,65-58,CEKOVSKY,MICHAL missed Tip In

    private val shot_missed_regex = "[^,]+,[^,]+,(.+) missed +(?:Dunk|Layup|Tip In)".r
    private val shot_missed_regex_new = "[^,]+,[^,]+,(.+), +2pt +(?:dunk|layup|alleyoop)(?:.* +)?missed".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(shot_missed_regex(player)) => Some(player)
      case Some(shot_missed_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  object ParseTwoPointerMade {
    // New:
    // Eric Ayala, 2pt jumpshot made
    // Legacy:
    // 18:49,43-27,YEBOAH,AKWASI made Two Point Jumper
    //(plus ParseRimMade)

    private val shot_made_regex = "[^,]+,[^,]+,(.+) made (?!Three|Free Throw).*".r
    private val shot_made_regex_new = "[^,]+,[^,]+,(.+), +2pt +(?:.* +)?made".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(shot_made_regex(player)) => Some(player)
      case Some(shot_made_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  object ParseTwoPointerMissed {
    // New:
    // Eric Ayala, 2pt jumpshot 2ndchance missed
    // Legacy:
    // 02:45,65-58,LINDSEY,SCOTTIE missed Two Point Jumper
    //(plus ParseRimMissed)

    private val shot_missed_regex = "[^,]+,[^,]+,(.+) missed (?!Three|Free Throw).*".r
    private val shot_missed_regex_new = "[^,]+,[^,]+,(.+), +2pt +(?:.* +)?missed".r
  //TODO
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(shot_missed_regex(player)) => Some(player)
      case Some(shot_missed_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  object ParseThreePointerMade {
    // New:
    // Eric Ayala, 3pt jumpshot made
    // Legacy:
    // SMITH,JALEN made Three Point Jumper

    private val shot_made_regex = "[^,]+,[^,]+,(.+) made Three Point.*".r
    private val shot_made_regex_new = "[^,]+,[^,]+,(.+), +3pt +(?:.* +)?made".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(shot_made_regex(player)) => Some(player)
      case Some(shot_made_regex_new(player)) => Some(player)
      case _ => None
    }
  }
  object ParseThreePointerMissed {
    // New:
    // Eric Ayala, 3pt jumpshot 2ndchance missed
    // Legacy:
    // DREAD,MYLES missed Three Point Jumper

    private val shot_missed_regex = "[^,]+,[^,]+,(.+) missed Three Point.*".r
    private val shot_missed_regex_new = "[^,]+,[^,]+,(.+), +3pt +(?:.* +)?missed".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(shot_missed_regex(player)) => Some(player)
      case Some(shot_missed_regex_new(player)) => Some(player)
      case _ => None
    }
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

  /** Uncategorized rebound, including team/deadball etc */
  object ParseRebound {
    // New:
    // Darryl Morsell, rebound defensive
    // Jalen Smith, rebound offensive
    // Team, rebound offensive team
    // 09:48:00	Team, rebound offensivedeadball
    // Legacy:
    // SMITH,JALEN Offensive Rebound
    // HARRAR,JOHN Defensive Rebound

    private val rebound_regex = "[^,]+,[^,]+,(.+) +(?:Offensive|Defensive|Deadball) +Rebound".r
    private val rebound_regex_new = "[^,]+,[^,]+,(.+), +rebound +.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(rebound_regex(player)) => Some(player)
      case Some(rebound_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Defensive rebound, including team/deadball etc */
  object ParseOffensiveRebound {
    // New:
    // Darryl Morsell, rebound defensive
    // 19:09:00,29-38,Team, rebound defensivedeadball
    // Legacy:
    // HARRAR,JOHN Defensive Rebound
    // (note legacy deadball rebounds aren't captured)

    private val rebound_regex = "[^,]+,[^,]+,(.+) +Offensive +Rebound".r
    private val rebound_regex_new = "[^,]+,[^,]+,(.+), +rebound +offensive.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(rebound_regex(player)) => Some(player)
      case Some(rebound_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Defensive rebound, including team/deadball etc */
  object ParseDefensiveRebound {
    // New:
    // Darryl Morsell, rebound defensive
    // 19:09:00,29-38,Team, rebound defensivedeadball
    // Legacy:
    // HARRAR,JOHN Defensive Rebound
    // (note legacy deadball rebounds aren't captured)

    private val rebound_regex = "[^,]+,[^,]+,(.+) +Defensive +Rebound".r
    private val rebound_regex_new = "[^,]+,[^,]+,(.+), +rebound +defensive.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(rebound_regex(player)) => Some(player)
      case Some(rebound_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Occurs with an intermediate FT miss (ignore),  a block out of bounds, missed
   * final FT off the defender? etc
   * We make this off or def because legacy format doesn't provide that information
  */
  object ParseDeadballRebound {
    // New:
    //04:28:0,52-59,Team, rebound offensivedeadball
    //04:28:0,52-59,Team, rebound deadballdeadball
    // Legacy:
    // 04:33,46-45,TEAM Deadball Rebound

    private val rebound_deadball_regex = "[^,]+,[^,]+,(.+) +Deadball +Rebound".r
    private val rebound_deadball_def_regex_new = "[^,]+,[^,]+,(.+), +rebound defensivedeadball".r
    def unapply(x: String): Option[String] =
      ParseOffensiveDeadballRebound.unapply(x).orElse {
        Option(x) match {
          case Some(rebound_deadball_regex(player)) => Some(player)
          case Some(rebound_deadball_def_regex_new(player)) => Some(player)
          case _ => None
        }
      }
  }

  /** Occurs with an intermediate FT miss (ignore),  a block out of bounds, missed
   * final FT off the defender? etc
   * Offensive only, but only works for new format
  */
  object ParseOffensiveDeadballRebound {
    // New:
    //04:28:0,52-59,Team, rebound offensivedeadball
    // Legacy:
    //(none)

    private val rebound_deadball_off_regex_new = "[^,]+,[^,]+,(.+), +rebound offensivedeadball".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(rebound_deadball_off_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  // Free throws

  /** Any made free throw */
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

  /** Any missed free throw */
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

  /** Any free throw attempt (missed or made) */
  object ParseFreeThrowAttempt {
    def unapply(x: String): Option[String] = ParseFreeThrowMissed.unapply(x).orElse {
      ParseFreeThrowMade.unapply(x)
    }
  }

  /** Presence of 1+ FTs in a possession (old format - will double count if split across clumps) */
  object ParseFreeThrowEvent {
    private val ft_start1_regex_new = "[^,]+,[^,]+,(.+), +freethrow 1of1 .*".r
    private val ft_start2_regex_new = "[^,]+,[^,]+,(.+), +freethrow 1of2 .*".r
    private val ft_start3_regex_new = "[^,]+,[^,]+,(.+), +freethrow 1of3 .*".r
    private val ft_missed_regex = "[^,]+,[^,]+,(.+) missed +Free Throw".r
    private val ft_made_regex = "[^,]+,[^,]+,(.+) made +Free Throw".r

    def unapply(x: String): Option[String] = Option(x) match {
      case Some(ft_start1_regex_new(player)) => Some(player)
      case Some(ft_start2_regex_new(player)) => Some(player)
      case Some(ft_start3_regex_new(player)) => Some(player)
      case Some(ft_missed_regex(player)) => Some(player)
      case Some(ft_made_regex(player)) => Some(player)
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

  /** Assist */
  object ParseAssist {
    // New:
    //18:28:00,0-0,Kyle Guy, assist
    // Legacy:
    //19:49,0-2,EDWARDS,CARSEN Assist
    private val assist_regex = "[^,]+,[^,]+,(.+) +Assist".r
    private val assist_regex_new = "[^,]+,[^,]+,(.+), +assist".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(assist_regex(player)) => Some(player)
      case Some(assist_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  // Foul events

  /** Personal foul (ie the person committing it)- can be offensive or defensive */
  object ParsePersonalFoul {
    // New:
    //RawGameEvent(Some("13:36:00,7-9,Jalen Smith, foul personal shooting;2freethrow"), None)
    // Legacy:
    //"opponent": "10:00,51-60,MYKHAILIUK,SVI Commits Foul"
    private val personal_foul_regex = "[^,]+,[^,]+,(?!TEAM)(.+) +Commits Foul".r
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
    // Closest is: "team": "09:31,44-48,TEAM Commits Foul" - only for coach technicals
    private val technical_foul_regex = "[^,]+,[^,]+,(TEAM) +Commits Foul".r
    private val technical_foul_regex_new = "[^,]+,[^,]+,(.+), +foul technical.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(technical_foul_regex(player)) => Some(player)
      case Some(technical_foul_regex_new(player)) => Some(player)
      case _ => None
    }
  }

  /** Flagrant foul */
  object ParseFlagrantFoul {
    // New:
    //"team": "03:42:00,60-67,Eric Carter, foul personal flagrant1;2freethrow"
    // Legacy:
    //(haven't found any yet)
    private val flagrant_foul_regex_new = "[^,]+,[^,]+,(.+), +foul personal flagrant.*".r
    def unapply(x: String): Option[String] = Option(x) match {
      case Some(flagrant_foul_regex_new(player)) => Some(player)
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

  /** An offensive event that tells us who is which side in a possession */
  object ParseOffensiveEvent {
    def unapply(x: String): Option[String] = x match {
      case ParseFreeThrowMade(x) => Some(x)
      case ParseFreeThrowMissed(x) => Some(x)
      case ParseShotMade(x) => Some(x)
      case ParseShotMissed(x) => Some(x)
      case ParseTurnover(x) => Some(x)
      case _ => None
      //(Note there's insufficient info in fouls to use them here)
      //(ORBs not needed becausee must be associated with one of the above actions)
    }
  }

  /** An offensive event that tells us who is which side in a possession */
  object ParseDefensiveActionEvent {
    def unapply(x: String): Option[String] = x match {
      case ParseDefensiveRebound(x) => Some(x)
      case _ => None
      //(Note there's insufficient info in fouls to use them here)
    }
  }

  /** A defensive event that provides context to an offensive action (eg turnovere) */
  object ParseDefensiveInfoEvent {
    def unapply(x: String): Option[String] = x match {
      case ParseShotBlocked(x) => Some(x)
      case ParseStolen(x) => Some(x)
      case _ => None
    }
  }

  /** Any defensive event */
  object ParseDefensiveEvent {
    def unapply(x: String): Option[String] =
      ParseDefensiveActionEvent.unapply(x).orElse { ParseDefensiveInfoEvent.unapply(x) }
  }
}
