package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._

object ExtractorUtils {

  //TODO: split on timeouts? (and have is_after_timeout flag, or sub_event == in-game/break/timeout)

  /** The length of the player code, eg AlPi==4, AlPiggoty==8 etc */
  val player_code_max_length = 16
  /** The max length of any one fragment, eg "MAMUKELASHVILI" is truncated to "MAMUKELASH" */
  val player_code_max_fragment_length = 10

  /** Error enrichment placeholder */
  val `parent_fills_in` = ""

  // Top level

  /** Converts a stream of partially parsed events into a list of lineup events
   * (note box_lineup has all players, but the top 5 are always the starters)
   */
  def build_partial_lineup_list(
    reversed_partial_events: Iterator[Model.PlayByPlayEvent],
    box_lineup: LineupEvent
  ): List[LineupEvent] = {
    val starters_only = box_lineup.copy(players = box_lineup.players.take(5))
    // Use this to render player names in their more readable format
    val tidy_ctx = LineupErrorAnalysisUtils.build_tidy_player_context(box_lineup)

    val starting_state = Model.LineupBuildingState(starters_only)
    val partial_events = reorder_and_reverse(reversed_partial_events)
    val end_state = partial_events.foldLeft(starting_state) { (state, event) =>

      /** Detect old format the first time a player name is encountered by looking for all upper case */
      def is_old_format(p: String, s: Model.LineupBuildingState): Option[Boolean] = s.old_format match {
        case None => Some(!p.exists(_.isLower))
        case _ => s.old_format //latched
      }
      def no_team_keyword(s: String): Boolean = s.toLowerCase != "team"

      event match {
        case Model.SubInEvent(min, _, player_name) if state.is_active(min) && no_team_keyword(player_name) =>
          val tidier_player_name = LineupErrorAnalysisUtils.tidy_player(player_name, tidy_ctx)
          val completed_curr = complete_lineup(state.curr, state.prev, min)
          state.copy(
            curr = new_lineup_event(
              completed_curr, in = Some(tidier_player_name)
            ),
            prev = completed_curr :: state.prev,
            old_format = is_old_format(player_name, state)
          )
        case Model.SubOutEvent(min, _, player_name) if state.is_active(min) =>
          val tidier_player_name = LineupErrorAnalysisUtils.tidy_player(player_name, tidy_ctx)
          val completed_curr = complete_lineup(state.curr, state.prev, min)
          state.copy(
            curr = new_lineup_event(
              completed_curr, out = Some(tidier_player_name)
            ),
            prev = completed_curr :: state.prev,
            old_format = is_old_format(player_name, state)
          )
        case Model.SubInEvent(min, _, player_name) if no_team_keyword(player_name) => // !state.is_active
          // Keep adding sub events
          val tidier_player_name = LineupErrorAnalysisUtils.tidy_player(player_name, tidy_ctx)
          state.with_player_in(tidier_player_name).copy(
            old_format = is_old_format(player_name, state)
          )

        case Model.SubOutEvent(min, _, player_name) => // !state.is_active
          // Keep adding sub events
          val tidier_player_name = LineupErrorAnalysisUtils.tidy_player(player_name, tidy_ctx)
          state.with_player_out(tidier_player_name).copy(
            old_format = is_old_format(player_name, state)
          )

        case Model.OtherTeamEvent(min, score, event_string) =>
          state.with_team_event(min, event_string).with_latest_score(score)

        case Model.OtherOpponentEvent(min, score, event_string) =>
          state.with_opponent_event(min, event_string).with_latest_score(score)

        case Model.GameBreakEvent(min, _) =>
          val completed_curr = complete_lineup(state.curr, state.prev, min)
          val (new_lineup_id, new_players) =
            if (state.old_format.getOrElse(box_lineup.team.year.value < 2018)) {
              (starters_only.lineup_id, starters_only.players)
            } else { // 2018+
              (completed_curr.lineup_id, completed_curr.players)
            }
          state.copy(
            curr = new_lineup_event(completed_curr).copy(
              lineup_id = new_lineup_id,
              players = new_players //reset lineup
            ),
            prev = completed_curr :: state.prev
          )
        case Model.GameEndEvent(min, _) =>
          state.copy(curr = complete_lineup(state.curr, state.prev, min))
      }
    }
    end_state.build()
  }

  // Utils with some exernal usefulness

  /** the list in parse_team_name can have seed numbers and results, eg '#10 Iowa (3-3)' */
  val extract_team_regex = "([#][0-9]+ +)?([^ ].*?)( *[(][0-9]+-[0-9]+[)])?".r

  /** Pulls team name from "title" table element, matching the target and opponent teams
    * returns the target team, the opposing team, and whether the target team is first (vs second)
  */
  def parse_team_name(teams: List[String], target_team: TeamId)
    : Either[ParseError, (String, String, Boolean)] =
  {
    val target_team_str = target_team.name
    teams.collect {
      case extract_team_regex(_, just_team, _) => just_team
    }.map(_.trim) match {
      case List(`target_team_str`, opponent) =>
        Right((target_team_str, opponent, true))

      case List(opponent, `target_team_str`) =>
        Right((target_team_str, opponent, false))

      case _ =>
      Left(ParseUtils.build_sub_error("team")(
        s"Could not find/match team names (target=[$target_team]): $teams"
      ))
    }
  }

  /** Gets the start time from the period - ie 2x 20 minute halves, then 5m overtimes */
  def start_time_from_period(period: Int): Double = (period - 1) match {
    case n if n < 2 => n*20.0
    case m => 40.0 + (m - 2)*5.0
  }
  /** Gets the end time (ie game duration to date) from the period
  *   - ie 2x 20 minute halves, then 5m overtimes
  */
  def duration_from_period(period: Int): Double = start_time_from_period(period + 1)

  /** Builds a player code out of the name, with various formats supported */
  def build_player_code(in_name: String, team: Option[TeamId]): LineupEvent.PlayerCodeId = {
    // Check full name vs map of misspellings
    def remove_accents(fragment: String): String = {
      import java.text.Normalizer
      Normalizer.normalize(fragment, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
    }
    val name = remove_accents(DataQualityIssues.misspellings(team).get(in_name).getOrElse(in_name))
    def first_last(fragment: String): String = {
      if (fragment.isEmpty) {
        ""
      } else {
        s"${fragment(0).toUpper}${fragment(fragment.length - 1).toLower}"
      }
    }
    def transform(fragment: String, max_len: Int): String = {
      if (fragment.isEmpty) {
        ""
      } else {
        s"${fragment(0).toUpper}${fragment.take(max_len).tail.toLowerCase}"
      }
    }
    def transform_first_name(fragment: String): String = {
      if (DataQualityIssues.players_with_duplicate_names(name.toLowerCase)) {
        first_last(fragment)
      } else {
        transform(fragment, 2)
      }
    }
    val code = ((name.split("\\s*,\\s*", 3).toList match {
      case all_name_set :: Nil =>
        all_name_set.split("\\s+").toList
      case last_name_set :: first_name_set :: Nil =>
        first_name_set.split("\\s+").toList ++ last_name_set.split("\\s+").toList
      case last_name_set :: suffix :: first_name_set :: Nil =>
        first_name_set.split("\\s+").toList ++ last_name_set.split("\\s+").toList ++ List(suffix)
      case _ => Nil //(impossible by construction of split)
    }).map { name_part =>
      val lower_case_name_part = name_part.toLowerCase.replace(".", "")
      // Misspelled fragments:
      DataQualityIssues.misspellings(team)
        .get(lower_case_name_part).getOrElse(lower_case_name_part)
        .take(player_code_max_fragment_length)
    } match {
      case head :: tail => // don't ever filter the head
        def name_filter(candidate: String): Boolean =
          candidate(0).isDigit ||
          candidate == "the" ||
          candidate == "first" || candidate == "second" || candidate == "third" ||
          candidate == "jr" ||
          candidate == "sr" ||
          candidate == "iv" || candidate == "vi" || //(that's enough surely??!)
            (candidate.startsWith("ii") && candidate.endsWith("ii"))

        List(head).filterNot(name_filter) ++ tail.filterNot { candidate => // get rid or jr/sr/ii/etc
          candidate.size < 2 || name_filter(candidate)
        }
      case Nil =>  Nil
    }) match {
      case Nil => ""
      case head :: Nil => transform(head, player_code_max_length)
      case head :: tail => //(tail is non Nil, hence tail.last is well-formed)
        val last_size = tail.last.size
        val leftover = player_code_max_length - last_size - 2
        // handle weird double-barreled names
        val middle = if (leftover >= 2) {
          val leftover_to_use = if (last_size < 6) { //short last name
            leftover // us as much as possible
          } else {
            2 //(treat like head)
          }
          transform(tail.reverse.drop(1).headOption.getOrElse(""), leftover_to_use)
        } else {
          ""
        }
        transform_first_name(head) + middle + transform(tail.last, player_code_max_length)
    }
    LineupEvent.PlayerCodeId(code, PlayerId(name))
  }

  // Internal Utils

  /** Orders play-by-play data to ensure that no subs occur in the middle of play-by-play
   * basically the order needs to be:
   * (non-sub-events) (sub-events) (non-sub events)
   * where ... all game events that reference players subbed out is in the first group,
   * all game events that reference players subbed in is in the second group, and
   * FTs in the same direction as a pre-sub foul go in the same direction
   * if neither of these things holds then events that occur lower/same score as the first sub
   * live in the first group, everything else lives in the second group
   * Simples!
   * Examples:
   * a) SUB_SET_1; REBOUND_X; SUB_SET_2 .. which should map to:
   *    REBOUND_X; SET_SET_1; SUB_SET_2 unless X is subbed in in one of the sub sets
   *                                    OR occurs with a higher score that SUB_SET1
   * b) B FREE THROW; A REBOUND; A ENTERS ... which should map to:
   *    B FREE THROW; A ENTERS; A REBOUND
   * BUT
   * c) A FREE THROW; A LEAVES ... should stay the same
   *
   * Actually most of the cases don't involve subs and are FT0related, examples:
   * (imagine these events come in totally random orders in practice!)

   00:23:10		67-76	Anthony Cowan, foul personal 2freethrow
   00:23:10	Aaron Jordan, foulon	67-76 <== (sometimes this name is wrong!)
   00:23:10		67-76	Anthony Cowan, substitution out
   00:23:10		67-76	Reese Mona, substitution in
   (lineup break)
   00:23:10	Aaron Jordan, freethrow 1of2 fastbreak made	67-77
   00:23:10	Tevian Jones, substitution in	67-77
   00:23:10	Adonis De La Rosa, substitution out	67-77
   00:23:10	Aaron Jordan, freethrow 2of2 fastbreak made	67-78

   or:
   13:45:00		26-41	Aaron Wiggins, 2pt stepbackjumpshot missed
   ---
   13:43:00		26-41	Jalen Smith, substitution out
   13:43:00		26-41	Ricky Lindo Jr., substitution in
   (opp sub)
   13:43:00		26-41	Team, rebound offensive team
   13:43:00		26-41	Darryl Morsell, substitution out
   13:43:00		26-41	Anthony Cowan, substitution in
   13:38:00		26-43	Eric Ayala, 2pt drivinglayup 2ndchance;pointsinthepaint made

   *
   * (protected just to support testing)
   */
  protected [ncaa] def reorder_and_reverse(
    reversed_partial_events: Iterator[Model.PlayByPlayEvent]
  ): List[Model.PlayByPlayEvent] = {
    /** Ensures subs don't enclose plays */
    def inner_sort(pre_ordered_block: List[Model.PlayByPlayEvent]): List[Model.PlayByPlayEvent] = {
      // first, order by score to get the weirdest cases out (and rank scoring shots earlier)
      val ordered_block = pre_ordered_block.sortBy {
        // events where the score increments live "in between" curr and next scores
        case ev: Model.MiscGameEvent if
          EventUtils.ParseFreeThrowMade.unapply(ev.event_string).nonEmpty ||
          EventUtils.ParseShotMade.unapply(ev.event_string).nonEmpty
        =>
          (ev.score.scored, ev.score.allowed, 0)

        // (subs live at the end)
        case ev: Model.SubOutEvent => (ev.score.scored, ev.score.allowed, 10)
        case ev: Model.SubInEvent => (ev.score.scored, ev.score.allowed, 10)

        case ev => (ev.score.scored, ev.score.allowed, 1)
      }
      val subs = (ordered_block.collect {
        case ev: Model.SubEvent => ev
      })
      if (subs.isEmpty) { // nothing to do
        ordered_block
      } else { //work to do to match up subs and events, see discussion in scaladoc
        val (sub_ins, sub_outs) = subs.partition {
          case _: Model.SubInEvent => true
          case _ => false
        }
        case class State(
          sub_score: Option[Game.Score],
          direction_team: Option[Boolean],
          group_1: List[Model.PlayByPlayEvent],
          group_2: List[Model.PlayByPlayEvent]
        ) {
          def build(): List[Model.PlayByPlayEvent] = {
            // (the foldLeft below on ordered_block reverses the outputs, so we reverse them again)
            group_1.reverse ++ subs ++ group_2.reverse
          }
        }
        /** Does the event reference a player who was subbed (in or out as param)? */
        def event_refs_player(ev: Model.MiscGameEvent, in_or_out: List[Model.SubEvent]): Boolean = {
          //(don't need to worry about case because these names are all extracted from the same PbP source)
          in_or_out.exists { candidate =>
            ev.event_string.contains(candidate.player_name)
          }
        }
        val starting_state = State(None, None, Nil, Nil)
        /** Adds to either the "pre-sub" list of the "post-sub" one based on state */
        def add_to_state(state: State, ev: Model.PlayByPlayEvent) = {
          def score_gt(s1: Game.Score, s2: Game.Score): Boolean = {
            import scala.math.Ordering.Implicits._
            Game.Score.unapply(s1).get > Game.Score.unapply(s2).get
          }
          if (state.sub_score.exists(score => score_gt(ev.score, score))) {
            state.copy(group_2 = ev :: state.group_2)
          } else {
            state.copy( //(log direction of pre-sub action, so can pull FTs from after the sub in)
              direction_team = Option(ev).collect {
                case ev: Model.MiscGameEvent
                  if EventUtils.ParseFreeThrowEvent.unapply(ev.event_string).nonEmpty ||
                      EventUtils.ParseShotMade.unapply(ev.event_string).nonEmpty ||
                      EventUtils.ParseFoulInfo.unapply(ev.event_string).nonEmpty
                  =>
                    ev.is_team_dir

                case ev: Model.MiscGameEvent
                  if EventUtils.ParseTechnicalFoul.unapply(ev.event_string).nonEmpty ||
                      EventUtils.ParsePersonalFoul.unapply(ev.event_string).nonEmpty
                  =>
                    !ev.is_team_dir
              }.orElse(state.direction_team),
              group_1 = ev :: state.group_1
            )
          }
        }
        (ordered_block.foldLeft(starting_state) { (state, next_event) =>
          next_event match {
            case ev: Model.SubEvent => //(already added to "subs")
              state.copy(sub_score = Some(ev.score))

            // Always put FTs tied to pre-sub events in the first group
            case ev: Model.MiscGameEvent
              if EventUtils.ParseFreeThrowAttempt.unapply(ev.event_string).nonEmpty &&
                state.direction_team.exists(_ == ev.is_team_dir)
            =>
              state.copy(group_1 = ev :: state.group_1)

            case ev: Model.OtherTeamEvent =>
              if (event_refs_player(ev, sub_ins)) {
                state.copy(group_2 = ev :: state.group_2)
              } else if (event_refs_player(ev, sub_outs)) {
                state.copy(group_1 = ev :: state.group_1)
              } else {
                add_to_state(state, ev)
              }

            case ev => // (game breaks and opponent events, leave well alone)
              //TODO: also need to handle opponent subs and re-ordering else
              // the advanced stats will be wrong
              add_to_state(state, ev)
          }
        }).build()
      }
    }

    val starting_state = List[List[Model.PlayByPlayEvent]]()
    def complete(in: List[List[Model.PlayByPlayEvent]]): List[Model.PlayByPlayEvent] = (in match {
      case Nil => Nil
      case last :: tail => inner_sort(last) :: tail
    }).flatten
    complete(reversed_partial_events.foldLeft(starting_state) { (acc, event) => acc match {
      case Nil =>
        // Create a new block of play by play events
        (event :: Nil) :: Nil
      case Nil :: tail =>
        // Create a new block of play by play events (in practice this won't happen)
        (event :: Nil) :: tail
      case (head :: inner_tail) :: outer_tail if head.min == event.min =>
        // Add the new play by play to the existing block
        (event :: head :: inner_tail) :: outer_tail
      case (curr @ (head :: inner_tail)) :: outer_tail => //head.min != event.min
        // Reorder the existing block then add a new one
        (event :: Nil) :: inner_sort(curr) :: outer_tail
    }})
  }

  /** Builds a lineup id from a list of players */
  private def build_lineup_id(players: List[LineupEvent.PlayerCodeId]): LineupEvent.LineupId = {
    LineupEvent.LineupId(players.map(_.code).sorted.mkString("_"))
  }

  /** Creates an "empty" new lineup - note "prev" has had "complete_lineup" called on it */
  private def new_lineup_event(
    prev: LineupEvent,
    in: Option[String] = None, out: Option[String] = None
  ): LineupEvent = {
    LineupEvent(
      date = prev.date.plusMillis((prev.duration_mins*60000.0).toInt),
      location_type = prev.location_type,
      start_min = prev.end_min,
      end_min = prev.end_min, //(updates with every event)
      duration_mins = 0.0, //(fill in at end of event)
      LineupEvent.ScoreInfo.empty.copy(
        start = prev.score_info.end,
        end = prev.score_info.end,
        start_diff = prev.score_info.end_diff
      ), //(complete later)
      team = prev.team,
      opponent = prev.opponent,
      lineup_id = LineupEvent.LineupId.unknown, //(will calc once we have all the subs)
      players = prev.players, //(will re-calc once we have all the subs)
      players_in = in.map(build_player_code(_, Some(prev.team.team))).toList,
      players_out = out.map(build_player_code(_, Some(prev.team.team))).toList,
      raw_game_events = Nil,
      team_stats = LineupEventStats.empty, //(calculate these 2 later)
      opponent_stats = LineupEventStats.empty
    )
  }

  /** Fills in/tidies up a partial lineup event following its completion */
  private def complete_lineup(curr: LineupEvent, prevs: List[LineupEvent], min: Double): LineupEvent = {
    val new_player_list = {
      val curr_players = curr.players.map(p => p.code -> p).toMap //(copied from the prev play)
      val tmp_players_out = curr.players_out.map(p => p.code -> p).toMap
      val tmp_players_in = curr.players_in.map(p => p.code -> p).toMap
      val poss1 = (curr_players -- tmp_players_out.keySet ++ tmp_players_in).values.toList
      val poss2 = (curr_players ++ tmp_players_in -- tmp_players_out.keySet).values.toList
      // Check for a common error case: player comes in and out in the same sub
      // Pick the right one based on what makes sense
      (poss1.size, poss2.size) match {
        case (5, _) => poss1
        case (_, 5) => poss2
        case _ =>
          // Try removing all common players:
          val common_players = tmp_players_in.keySet.filter(tmp_players_out.keySet)
          val alt_players_in = tmp_players_in -- common_players
          val alt_players_out = tmp_players_out -- common_players
          val poss3 = (curr_players -- alt_players_out.keySet ++ alt_players_in).values.toList
          poss3
      }
    }
    //TODO test the lineup fix logic

    curr.copy(
      end_min = min,
      duration_mins = min - curr.start_min,
      score_info = curr.score_info.copy(
        end_diff = curr.score_info.end.scored - curr.score_info.end.allowed
      ),
      team_stats = curr.team_stats.copy(
        num_events = curr.raw_game_events.filter(_.team.isDefined).size,
        num_possessions = 0 //(calculate later)
      ),
      opponent_stats = curr.opponent_stats.copy(
        num_events = curr.raw_game_events.filter(_.opponent.isDefined).size, //TODO exclude subs
        num_possessions = 0 //(calculate later)
      ),
      lineup_id = build_lineup_id(new_player_list),
      players = new_player_list.sortBy(_.code),
      players_in = curr.players_in.reverse,
      players_out = curr.players_out.reverse,
      raw_game_events = curr.raw_game_events.reverse
    )
  }

  // Models (used by the parser also)

  object Model {
    private val SUB_SAFETY_DELTA_MINS = 4.0/60 //4s

    /** State for building raw line-up data */
    private [ExtractorUtils] case class LineupBuildingState(
      curr: LineupEvent,
      prev: List[LineupEvent] = Nil,
      old_format: Option[Boolean] = None // most 2018+ is new format but there are a few exceptions
    ) {
      def build(): List[LineupEvent] = {
        (curr :: prev).reverse
      }
      /** Opposition subs are currently treated as game events. but shouldn't
       *  result in new lineups */
      private def is_sub(raw: LineupEvent.RawGameEvent): Boolean = raw.opponent.map { s =>
        val s_lower = s.toLowerCase.trim
        //TODO: move this into some parsing module
        s_lower.endsWith("leaves game") || s_lower.endsWith("enters game") ||
        s_lower.endsWith("substitution in") || s_lower.endsWith("substitution out")
      }.getOrElse(false)

      /** Ifsome time has elapsed since the last sub or a game event has occurred */
      def is_active(min: Double): Boolean =
        curr.raw_game_events.filterNot(is_sub).nonEmpty ||
        {
          min - curr.end_min > SUB_SAFETY_DELTA_MINS
        }

      // State manipulation

      def with_player_in(player_name: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            players_in = build_player_code(player_name, Some(curr.team.team)) :: curr.players_in
          )
        )
      def with_player_out(player_name: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            players_out = build_player_code(player_name, Some(curr.team.team)) :: curr.players_out
          )
        )
      def with_latest_score(score: Game.Score): LineupBuildingState = {
        copy(
          curr = curr.copy(
            score_info = curr.score_info.copy(
              end = score
            )
          )
        )
      }
      def with_team_event(min: Double, event_string: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            end_min = min,
            raw_game_events = LineupEvent.RawGameEvent.team(event_string, min) :: curr.raw_game_events
          )
        )
      def with_opponent_event(min: Double, event_string: String): LineupBuildingState =
        copy(
          curr = curr.copy(
            end_min = min,
            raw_game_events = LineupEvent.RawGameEvent.opponent(event_string, min) :: curr.raw_game_events
          )
        )
    }

    // Event model
    sealed trait PlayByPlayEvent {
      /** The ascending minute of the game */
      def min: Double
      /** Update the minute (eg to switch from descending to ascending internal)*/
      def with_min(new_min: Double): PlayByPlayEvent
      /** The current score */
      def score: Game.Score
    }
    // Wildcard events
    sealed trait MiscGameEvent extends PlayByPlayEvent {
      /** The raw event string */
      def event_string: String

      /** Whether the event is in the team direction */
      def is_team_dir: Boolean
    }
    sealed trait SubEvent extends PlayByPlayEvent {
      /** The raw or processed substitute name */
      def player_name: String
    }
    sealed trait MiscGameBreak extends PlayByPlayEvent
    // Concrete
    case class SubInEvent(min: Double, score: Game.Score, player_name: String) extends SubEvent {
      def with_min(new_min: Double): SubInEvent = copy(min = new_min)
    }
    case class SubOutEvent(min: Double, score: Game.Score, player_name: String) extends SubEvent {
      def with_min(new_min: Double): SubOutEvent = copy(min = new_min)
    }
    case class OtherTeamEvent(min: Double, score: Game.Score, event_string: String) extends MiscGameEvent {
      def with_min(new_min: Double): OtherTeamEvent = copy(min = new_min)
      def is_team_dir: Boolean = true
    }
    case class OtherOpponentEvent(min: Double, score: Game.Score, event_string: String) extends MiscGameEvent {
      def with_min(new_min: Double): OtherOpponentEvent = copy(min = new_min)
      def is_team_dir: Boolean = false
    }
    case class GameBreakEvent(min: Double, score: Game.Score) extends MiscGameBreak {
      def with_min(new_min: Double): GameBreakEvent = copy(min = new_min)
    }
    case class GameEndEvent(min: Double, score: Game.Score) extends MiscGameBreak {
      def with_min(new_min: Double): GameEndEvent = copy(min = new_min)
    }
  }
}
