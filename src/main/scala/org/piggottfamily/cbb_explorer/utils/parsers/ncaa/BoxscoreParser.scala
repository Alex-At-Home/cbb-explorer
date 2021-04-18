package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._
import cats.implicits._
import cats.data._
import com.github.dwickern.macros.NameOf._
import shapeless._
import ops.hlist._
import shapeless.labelled._
import record._
import ops.record._
import syntax.singleton._
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.{Try, Success, Failure}

/** Parses the game HTML (or game subsets of the team HTML) */
trait BoxscoreParser {

  import ExtractorUtils._

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `ncaa.parse_boxscore` = "ncaa.parse_boxscore"

  // Holds all the HTML parsing logic
  protected object builders {

    def team_finder(doc: Document): List[String] = //2020+ is 40%, 2019- is 50%
      (doc >?> elementList("div#contentarea table.mytable[width~=[45]0%] td a[href]"))
        .getOrElse(Nil).map(_.text)

    def score_finder(doc: Document): List[String] = //2020+ is 40%, 2019- is 50%
      (doc >?> elementList("div#contentarea table.mytable[width~=[45]0%] td[align=right]"))
        .getOrElse(Nil).map(_.text)

    def date_finder(doc: Document): Option[String] =
      (doc >?> element("td.boldtext:contains(Game Date:) + td")).map(_.text)

    def boxscore_finder(doc: Document, target_team_first: Boolean): Option[List[Element]] = target_team_first match {
      case true =>
        (doc >?> elementList("div#contentarea div.header_menu + table.mytable[width=1000px] td a[href]")).filter(_.nonEmpty)
      case false =>
        (doc >?> elementList("div#contentarea br + table.mytable[width=1000px] td a[href]")).filter(_.nonEmpty)
    }
  }

  /** Gets the boxscore lineup from the HTML page (external roster has either just names or names + numbers) */
  def get_box_lineup(
    filename: String, in: String, team_id: TeamId,
    external_roster: Either[List[String], Map[String, String]] = Left(Nil), neutral_game_dates: Set[String] = Set()
  ): Either[List[ParseError], LineupEvent] =
  {
    val browser = JsoupBrowser()

    // Error reporters
    val doc_request_builder = ParseUtils.build_request[Document](`ncaa.parse_boxscore`, filename) _
    val single_error_completer = ParseUtils.enrich_sub_error(`ncaa.parse_boxscore`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      period <- parse_period_from_filename(filename)
                  .left.map(single_error_completer)

      team_info <- parse_team_name(
        builders.team_finder(doc), team_id
      ).left.map(single_error_completer)

      (team, opponent, target_team_first) = team_info //SI-5589

      maybe_date_str = builders.date_finder(doc)

      date <- parse_date(
        maybe_date_str
      ).left.map(single_error_completer)

      year = Year(if (date.monthOfYear.get >= 6) date.year.get else (date.year.get - 1))

      location_type = maybe_date_str.map(_.split(" ")(0)) match { //(get rid of optiomnal time)
        case Some(date_str) if neutral_game_dates.contains(date_str) => Game.LocationType.Neutral
        case _ if target_team_first => Game.LocationType.Away
        case _ if !target_team_first => Game.LocationType.Home
      }

      final_score <- parse_final_score(
        builders.score_finder(doc), target_team_first
      ).left.map(single_error_completer)

      starting_lineup_from_box <- parse_players_from_boxscore(
        builders.boxscore_finder(doc, target_team_first)
      ).left.map(single_error_completer)

      manual_extra_players =
        DataQualityIssues.players_missing_from_boxscore.getOrElse(TeamId(team), Map()).getOrElse(year, List())

      starting_lineup = external_roster match {
        case Left(others) => // Have a list of players from box score
          val starting_lineup_from_box_set = starting_lineup_from_box.toSet

          starting_lineup_from_box ++ (if (others.isEmpty) {
            manual_extra_players
          } else { //(in this case, already have the missing players from box score)
            others
          }).filterNot(starting_lineup_from_box_set)

        case Right(number_to_player_map) => // Have a canonical list from the roster
          val just_players = number_to_player_map.values.toList
          val just_players_set = just_players.toSet
          // We're going to validate each entry in the box
          // If it's not in the roster then we try to match it fuzzily against the roster
          val validated_starting_lineup = starting_lineup_from_box.map { player =>
            if (just_players_set(player)) {
              player
            } else {
              DataQualityIssues.Fixer.fuzzy_box_match(
                player, just_players, s"${team}:box"
              ) match {
                case Right(box_name) => box_name
                case Left(_) => player //(just stick with what we have)
              }

            }
          }
          val validated_starting_lineup_set = validated_starting_lineup.toSet
          validated_starting_lineup ++
            just_players.filterNot(validated_starting_lineup_set) ++ manual_extra_players
      }

      validated_lineup <- validate_box_score(
        TeamId(team), starting_lineup
      ).left.map(single_error_completer)

    } yield LineupEvent(
      date,
      location_type,
      start_min = start_time_from_period(period, is_women_game = false), //(doesn't matter which)
      end_min = start_time_from_period(period, is_women_game = false),
      duration_mins = 0.0,
      score_info = LineupEvent.ScoreInfo(Game.Score(0, 0), final_score, 0, 0),
      team = TeamSeasonId(TeamId(team), year),
      opponent = TeamSeasonId(TeamId(opponent), year),
      lineup_id = LineupEvent.LineupId.unknown,
      players = validated_lineup,
      players_in = Nil,
      players_out = external_roster match { // Naughtily override this to give numbers in the code
        case Right(number_to_player_map) =>
          number_to_player_map.toList.map { case (number, name) =>
            LineupEvent.PlayerCodeId(number, PlayerId(name))
          }
        case _ => Nil
      },
      raw_game_events = Nil,
      team_stats = LineupEventStats.empty,
      opponent_stats = LineupEventStats.empty
    )
  }

  // Utils
  /** Gets the box score's period from the filename */
  protected def parse_period_from_filename(filename: String)
    : Either[ParseError, Int] =
  {
    val filename_parser = "[^_]+_p([0-9]+)[.][^.]*".r // eg test_p<period>.html

    filename match {
      case filename_parser(period_str) =>
        Right(period_str.toInt)
      case _ => // default to period 1
        Right(1)
    }
  }

  /** Parses dates of the format '12/03/2017' */
  protected def parse_date(date: Option[String]):
    Either[ParseError, DateTime] =
  {
    val formatter  = DateTimeFormat.forPattern("MM/dd/yyyy")

    date.map(_.trim).map { date_str =>
      Try(
        //(the split gets rid of the optional time at the end of the date)
        Right(
          formatter.parseDateTime(date_str.split(" ")(0))
            .withHourOfDay(17) // (make it an early evening game, no reason)
        )
      ).toOption.getOrElse {
        Left(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Unexpected date format: [$date_str]"
        ))
      }
    }.getOrElse {
      Left(ParseUtils.build_sub_error(`parent_fills_in`)(
        s"Could not find date"
      ))
    }
  }

  protected def parse_final_score(scores_per_period: List[String], target_team_first: Boolean)
    : Either[ParseError, Game.Score]=
  {
    val num_scores = scores_per_period.size
    if ((num_scores >= 2) && ((num_scores % 2) == 0)) {
      val first_score = scores_per_period(num_scores/2 - 1)
      val second_score = scores_per_period(num_scores - 1)

      (Try(first_score.toInt), Try(second_score.toInt)) match {
        case (Success(score1), Success(score2)) if target_team_first =>
          Right(Game.Score(score1, score2))
        case (Success(score1), Success(score2)) if !target_team_first =>
          Right(Game.Score(score2, score1))
        case error =>
          Left(ParseUtils.build_sub_error(`parent_fills_in`)(
            s"Unexpected score format [one of the scores not integer]: [$error]"
          ))
      }

    } else {
      Left(ParseUtils.build_sub_error(`parent_fills_in`)(
        s"Unexpected score format [odd number of values]: [$scores_per_period]"
      ))
    }
  }

  /** Gets the list of starters from the boxscore */
  protected def parse_players_from_boxscore(boxscore_table: Option[List[Element]])
    : Either[ParseError, List[String]] =
  {
    boxscore_table.map { rows =>
      if (rows.size >= 5) {
        Right(rows.map(_.text))
      } else {
        Left(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Not enough rows in boxscore table: [$rows]"
        ))
      }
    }.getOrElse {
      Left(ParseUtils.build_sub_error(`parent_fills_in`)(
        s"Could not find boxscore table"
      ))
    }
  }

  /** Checks there are no duplicates in the lineup */
  protected def validate_box_score(team: TeamId, lineup: List[String]):
    Either[ParseError, List[LineupEvent.PlayerCodeId]] =
  {
    def has_dups(l: List[LineupEvent.PlayerCodeId]): Boolean = {
      l.size != l.map(_.code).toSet.size
    }
    lineup.map(build_player_code(_, Some(team))) match {
      case l if has_dups(l) =>
        Left(ParseUtils.build_sub_error(`parent_fills_in`)(
          s"Duplicate players: [$l]"
        ))
      case l => Right(l)
    }
  }
}
object BoxscoreParser extends BoxscoreParser
