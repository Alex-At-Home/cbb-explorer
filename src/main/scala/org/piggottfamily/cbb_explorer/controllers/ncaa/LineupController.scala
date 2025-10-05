package org.piggottfamily.cbb_explorer.controllers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa._

import LineupController._

import java.nio.file.{Path, Paths}
import scala.util.matching.Regex
import scala.util.{Try, Success, Failure}

import com.softwaremill.quicklens._
import cats.implicits._
import cats.data._

/** Top level business logic for parsing the different datasets */
class LineupController(d: Dependencies = Dependencies()) {

  /** Builds up a list of a team's good lineups (logging and returning errored
    * lineups)
    */
  def build_team_lineups(
      root_dir: Path,
      team: TeamId,
      game_id_filter: Option[Regex] = None,
      min_time_filter: Option[Long] = None
  ): (
      List[LineupEvent],
      List[LineupEvent],
      List[PlayerEvent],
      List[ShotEvent]
  ) = {
    sealed trait LineupError
    case class FileError(f: Path, ex: Throwable) extends LineupError
    case class ParserError(f: Path, l: List[ParseError]) extends LineupError

    // Get neutral game dates (and check if we are running vs new or old format games)
    def build_neutral_games(
        team_filename: String,
        team_html: String,
        format_version: Int
    ): Option[Set[String]] =
      TeamScheduleParser.get_neutral_games(
        team_filename,
        team_html,
        format_version
      ) match {
        case Left(error) =>
          if (format_version > 0) // (will retry with version 1)
            d.logger.info(
              s"[format_version=$format_version] Failed to parse neutral games: [$error]"
            )
          None // (carry on)
        case Right((checked_team, neutral_set)) if checked_team == team =>
          d.logger.info(
            s"[format_version=$format_version]  Neutral game dates: [$neutral_set]"
          )
          Some(neutral_set)
        case wtf @ _ => // (not the right team, ignore)
          None
      }

    val (neutral_games, team_fileid) = (for {
      team_filename <- d.file_manager
        .list_files(root_dir / teams_dir, Some("html"))
        .iterator
      team_html = d.file_manager.read_file(team_filename)

      format_results <- build_neutral_games(
        team_filename.last,
        team_html,
        format_version = 0
      ) match {
        case None => // try with new format
          build_neutral_games(team_filename.last, team_html, format_version = 1)
        case results =>
          results
      }
      tmp_team_fileid = team_filename.last.split("[.]")(0) // (remove extension)

    } yield format_results -> Some(tmp_team_fileid))
      .take(1)
      .toList
      .headOption
      .getOrElse {
        d.logger.info(
          s"Failed to find the schedule in [${root_dir / teams_dir}]"
        )
        Set[String]() -> None
      }

    // Get lineups for selected games

    val file_filter = min_time_filter.map { min_time => (file_time: Long) =>
      {
        file_time > min_time
      }
    }

    // Try to get the most accurate canonical list of players
    // First look for a roster - if that doesn't exist (legacy) just get all the box scores
    val (external_roster, derived_format_version) =
      build_roster(root_dir, team, team_fileid)

    // println(s"All_box_roster_players: [$external_roster]")

    val games_iterator = for {
      // (early in the season games might not exist)
      game <- derived_format_version match {
        case 0 =>
          Try(
            d.file_manager.list_files(
              root_dir / play_by_play_dir,
              Some("html"),
              file_filter
            )
          ).getOrElse(Nil).iterator
        case _ =>
          Try(
            d.file_manager.list_files(
              root_dir / contests_dir,
              Some("html"),
              file_filter,
              recursive = true
            )
          ).getOrElse(Nil).iterator.filter { p =>
            p.last == v1_pbp_filename
          }
      }

      game_id = derived_format_version match {
        case 0 =>
          game.last.split("[.]")(0)
        case _ =>
          (game / up).last // (dirname is gameid in version 1)
      }
      if game_id_filter.forall(_.findFirstIn(game_id).isDefined)
    } yield game_id -> game

    val lineups = for {
      (game_id, game) <- games_iterator

      _ = d.logger.info(s"Reading [$game]: [$game_id]")

      lineup = Try {
        build_game_lineups(
          root_dir,
          game_id,
          team,
          external_roster,
          neutral_games,
          derived_format_version
        )
      } match {
        case Success(res) => res.left.map { errs => ParserError(game, errs) }
        case Failure(ex)  => Left(FileError(game, ex))
      }
    } yield lineup // (good, bad, player_event) triples

    case class State(
        good_lineups: List[LineupEvent],
        bad_lineups: List[LineupEvent],
        player_events: List[PlayerEvent],
        shot_events: List[ShotEvent]
    )

    /** Sort games for debugging purposes - turn off otherwise to save on memory
      * (lineups is an iterator)
      */
    val debug_sort = false
    val maybe_sorted_lineups = if (debug_sort) lineups.toList.sortBy {
      case Right((good, _, _, _)) =>
        good.headOption.map(_.date.getMillis).getOrElse(0L)
      case Left(_) => 0L
    }
    else lineups

    val end_state = maybe_sorted_lineups.foldLeft(State(Nil, Nil, Nil, Nil)) {
      (state, lineup_info) =>
        lineup_info match {
          case Right((good, bad, player, shot)) =>
            d.logger.info(
              s"Successful parse: good=[${good.size}] bad=[${bad.size}]"
            )
            // (we'll sort )
            state.copy(
              good_lineups = state.good_lineups ++ good,
              bad_lineups = state.bad_lineups ++ bad,
              player_events = state.player_events ++ player,
              shot_events = state.shot_events ++ shot
            )
          case Left(FileError(game, ex)) =>
            d.logger.info(s"File error with [$game]: [$ex]")
            state
          case Left(ParserError(game, errors)) =>
            d.logger.info(s"Parse error with [$game]: [$errors]")
            state
        }
    }
    (
      end_state.good_lineups,
      end_state.bad_lineups,
      end_state.player_events,
      end_state.shot_events
    )
  }

  /** Gets a list of Roster Entry objects, plus any box players missing from
    * that list
    */
  def build_roster(
      root_dir: Path,
      team: TeamId,
      team_fileid: Option[String] = None,
      include_coach: Boolean = false,
      unify_ncaa_ids: Boolean = false
  ): ((List[String], List[RosterEntry]), Int) = {
    val (roster_players, format_version) = ((Try(
      d.file_manager.list_files(
        root_dir / roster_dir,
        Some("html"),
        None,
        recursive = true
      ) -> 0
    ).getOrElse(Nil -> 1) match {
      case (Nil, _) if team_fileid.isDefined =>
        d.file_manager.list_files(
          root_dir / teams_dir / RelPath(
            team_fileid.getOrElse("__internal_logic_error__")
          ),
          Some("html"),
          None,
          recursive = true
        ) -> 1
      case other => other
    }) match {
      case (file :: _, format_version) =>
        val roster_html = d.file_manager.read_file(file)
        val roster_lineup = RosterParser
          .parse_roster(
            file.last.toString,
            roster_html,
            team,
            format_version,
            include_coach
          )
          .map { roster_entries =>
            roster_entries.map { roster_entry =>
              // If there is a player file for this roster entry then parse it to get unified NCAA id
              roster_entry.player_code_id.ncaa_id match {
                case Some(ncaa_id) if unify_ncaa_ids =>
                  val player_html = d.file_manager.read_file(
                    root_dir / players_dir / s"${ncaa_id}.html"
                  )
                  val maybe_unified_ncaa_id = RosterParser
                    .get_unified_ncaa_id(ncaa_id, player_html)
                    .toOption
                    .flatten

                  // DEBUG
                  // println(
                  //   s"******* [${roster_entry.player_code_id}] USE [$maybe_unified_ncaa_id]"
                  // )

                  roster_entry.copy(
                    player_code_id = roster_entry.player_code_id
                      .copy(ncaa_id =
                        maybe_unified_ncaa_id.orElse(Some(ncaa_id))
                      ) // (fallback to existing NCAA id))
                  )
                case _ =>
                  roster_entry
              }
            }
          }

        roster_lineup.left.foreach { errors =>
          d.logger.error(
            s"Parse error with [$format_version][$team][$root_dir]: [$errors]"
          )
        }
        roster_lineup.toOption.map(_ -> format_version)
      case _ => None
    }).getOrElse(Nil -> 0)

    // Now read all the box scores in to get any missing names:

    val all_box_players =
      (for {
        box <- format_version match {
          case 0 =>
            Try(
              d.file_manager.list_files(
                root_dir / boxscore_dir,
                Some("html"),
                None
              )
            ).getOrElse(Nil).iterator
          case _ =>
            Try(
              d.file_manager.list_files(
                root_dir / contests_dir,
                Some("html"),
                None,
                recursive = true
              )
            ).getOrElse(Nil).iterator.filter {
              _.last == v1_boxscore_filename
            }
        }
        box_html = d.file_manager.read_file(box)
        box_lineup <- (d.boxscore_parser.get_box_lineup(
          box.last.toString,
          box_html,
          team,
          format_version,
          (Nil, roster_players)
        ) match {
          case Right(lineup) => Some(lineup)
          case _             => None
        })
      } yield box_lineup.players.map(_.id.name)).flatten.toSet.toList

    ((all_box_players, roster_players), format_version)
  }

  /** Given a game/team id, returns good and bad paths for that game */
  protected def build_game_lineups(
      root_dir: Path,
      game_id: String,
      team: TeamId,
      external_roster: (List[String], List[RosterEntry]),
      neutral_game_dates: Set[String],
      format_version: Int
  ): Either[List[
    ParseError
  ], (List[LineupEvent], List[LineupEvent], List[PlayerEvent], List[ShotEvent])] = {
    val (playbyplay_path, boxscore_path, maybe_shot_event_path) =
      format_version match {
        case 0 =>
          val playbyplay_filename = s"$game_id.html"
          val boxscore_filename =
            s"${game_id}42b2.html" // (encoding of 1st period box score)
          (
            root_dir / play_by_play_dir / playbyplay_filename,
            root_dir / boxscore_dir / boxscore_filename,
            None
          )
        case _ =>
          (
            root_dir / contests_dir / game_id / v1_pbp_filename,
            root_dir / contests_dir / game_id / v1_boxscore_filename,
            Some(root_dir / contests_dir / game_id / v1_shotlocs_filename)
          )
      }

    val play_by_play_html = d.file_manager.read_file(playbyplay_path)
    val box_html = d.file_manager.read_file(boxscore_path)
    val maybe_shot_event_html =
      maybe_shot_event_path.map(d.file_manager.read_file)
    for {
      tmp_box_lineup <- d.boxscore_parser.get_box_lineup(
        boxscore_path.last,
        box_html,
        team,
        format_version,
        external_roster,
        neutral_game_dates
      )
      sorted_pbp_events <-
        if (format_version > 0) { // (used below in a couple of places)
          d.playbyplay_parser
            .get_sorted_pbp_events(
              playbyplay_path.last,
              play_by_play_html,
              tmp_box_lineup,
              format_version
            )
        } else Right(Nil)

      box_lineup =
        if (format_version > 0) {
          PlayByPlayUtils.inject_starting_lineup_into_box(
            sorted_pbp_events,
            tmp_box_lineup,
            external_roster,
            format_version
          )
        } else {
          tmp_box_lineup
        }

      _ = d.logger.info(
        s"Parsed box score: opponent=[${box_lineup.opponent}] venue=[${box_lineup.location_type}]"
      )

      lineup_events <- d.playbyplay_parser.create_lineup_data(
        playbyplay_path.last,
        play_by_play_html,
        box_lineup,
        format_version
      )

      List(player_events_good, player_events_bad) = List(
        lineup_events._1,
        lineup_events._2
      ).map { ls =>
        // (note we are including bad lineups in our player events since it's not such a disaster -
        // what we care about is mostly the individual stats)
        ls.map { l =>
          val player_events = LineupUtils.create_player_events(l, box_lineup)
          // Now finally transform lineup events with the roster info from player events
          val lineup_with_shot_info = l
            .modify(_.team_stats.player_shot_info)
            .setTo(
              LineupUtils.sum_shot_infos(
                player_events.flatMap(_.player_stats.player_shot_info.toList)
              )
            )
          (
            lineup_with_shot_info,
            player_events.map(
              modify[PlayerEvent](_.player_stats.player_shot_info).setTo(None)
            )
            // (remove shot info stats now that we've copied them across)
          )
        }
      }

      raw_shot_events =
        (maybe_shot_event_path, maybe_shot_event_html).mapN((_, _)) match {
          case Some((path, html)) =>
            d.shot_parser
              .create_shot_event_data(
                path.last,
                html,
                box_lineup
              )
              .leftMap { errors =>
                // (some games don't have shot data, that's fine, we'll log the info and carry on)
                d.logger.error(
                  s"[c_s_e_d] WARNING failed to parse shot events for [$game_id][$path]: [${errors
                      .map(err => s"${err.id}: ${err.messages.map(_.substring(0, 32))}")}]"
                )
                errors
              }
              .getOrElse(Nil)
          case None => Nil
        }

      enriched_shot_events = PlayByPlayUtils.enrich_shot_events_with_pbp(
        raw_shot_events,
        sorted_pbp_events,
        // (the error analysis/fix messes up the sorting of the lineups, so redo explicitly)
        lineup_events._1.sortBy(_.start_min),
        lineup_events._2.sortBy(_.start_min),
        box_lineup
      )
    } yield (
      player_events_good.map(_._1), // good lineups adjusted with player info
      player_events_bad.map(_._1), // bad lineups adjusted with player info
      player_events_good.map(_._2).flatten ++ player_events_bad
        .map(_._2)
        .flatten, // player info only
      enriched_shot_events
    )
  }

}

object LineupController {

  val teams_dir = RelPath("teams")
  val players_dir = RelPath("players")
  val play_by_play_dir = RelPath("game") / RelPath("play_by_play")
  val boxscore_dir = RelPath("game") / RelPath("box_score")
  val contests_dir = RelPath("contests")
  val roster_dir = RelPath("team")

  val v1_boxscore_filename = "individual_stats.html"
  val v1_pbp_filename = "play_by_play.html"
  val v1_shotlocs_filename = "box_score.html"

  /** Dependency injection */
  case class Dependencies(
      boxscore_parser: BoxscoreParser = BoxscoreParser,
      playbyplay_parser: PlayByPlayParser = PlayByPlayParser,
      shot_parser: ShotEventParser = ShotEventParser,
      logger: LogUtils = LogUtils,
      file_manager: FileUtils = FileUtils
  )
}
