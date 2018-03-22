package org.piggottfamily.cbb_explorer.utils.parsers.kenpom

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.utils.parsers._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._

/** Parses the team HTML */
trait TeamParser {

  protected val `kenpom.parse_team` = "kenpom.parse_team"
  protected val `kenpom.parse_team.parse_game` = "kenpom.parse_team.parse_game"

  /**
   * Parses HTML representing a team's season
   */
  def parse_team(in: String, filename: String): Either[List[ParseError], ParseResponse[TeamSeason]] = {

    val browser = JsoupBrowser()

    // Error reporters:
    val doc_request_builder = ParseUtils.build_request[Document](`kenpom.parse_team`, filename) _
    val single_error_enricher = ParseUtils.enrich_sub_error(`kenpom.parse_team`, filename) _
    val multi_error_enricher = ParseUtils.enrich_sub_errors(`kenpom.parse_team`, filename) _

    for {
      doc <- doc_request_builder(browser.parseString(in))

      // Get team name and year
      team_season <- parse_filename(filename).left.map(multi_error_enricher)

      // Get coach
      coach_or_errors = parse_coach(doc).left.map(single_error_enricher)

      // Get basic ranking and metrics
      metrics_or_errors = parse_metrics(doc).left.map(multi_error_enricher)

      // TODO Get games
      // TODO Get players

      coach_metrics <- ParseUtils.lift(coach_or_errors, metrics_or_errors)
      (coach, metrics) = coach_metrics //SI-5589

    } yield ParseResponse(TeamSeason(
      team_season, metrics, Nil, Map.empty, coach
    ))
  }

  /** Extracts the team name and year from the filename */
  protected def parse_filename(filename: String): Either[List[ParseError], TeamSeasonId] = {
    /** The filename spat out by webhttrack */
    val FilenameRegex = "team[0-9a-f]{4}(20[0-9]{2})?_([^_]+)?.*[.]html".r

    filename match {
      case FilenameRegex(year, team_name) =>
        val errors = List("year" -> year, "team_name" -> team_name).collect {
          case (fieldname, value) if value == null =>
            ParseUtils.build_sub_error(fieldname)(
              s"Failed to parse [$filename] - couldn't extract [$fieldname]"
            )
        }
        errors match {
          case Nil => Right(TeamSeasonId(TeamId(team_name), Year(year.toInt)))
          case _ => Left(errors)
        }
      case _ =>
        Left(List(ParseUtils.build_sub_error("")(
          s"Completely failed to parse [$filename] to extract year and team"
        )))
    }
  }

  /** Parses out the coach id from the top level HTML */
  protected def parse_coach(doc: Document): Either[ParseError, CoachId] = {
    (doc >?> element("span[class=coach]") >?> element("a")).flatten.headOption match {
      case Some(coach) =>
        Right(CoachId(coach.toString))
      case None =>
        Left(ParseUtils.build_sub_error("coach")(
          s"Failed to parse HTML - couldn't extract [coach]"
        ))
    }
  }

  protected def parse_metrics(doc: Document): Either[List[ParseError], TeamSeasonStats] = {
    Left(Nil)
  }

  /**
   * Parses HTML fragment representing a team's games
   */
  protected def parse_games(): Either[List[ParseError], ParseResponse[List[Game]]] = {
    Left(Nil)
  }
  /**
   * Parses HTML fragment representing a single game
   */
  protected def parse_game(): Either[List[ParseError], Game] = {
    // Get opponent
    // Get result
    // Get my ranking
    // Get opponent ranking
    // Get location type
    // Get primary tier
    // Get secondary tiers
    Left(Nil)
  }
}
object TeamParser extends TeamParser

// case class TeamSeason(
// 	team_season: TeamSeasonId,
// 	stats: TeamSeasonStats,
// 	games: List[Game],
// 	players: Map[PlayerId, PlayerSeason],
// 	coach: CoachId)
// case class TeamSeasonId(team: TeamId, year: Year)
// case class TeamSeasonStats(
// 	adj_margin: Metric,
// 	adj_off: Metric,
// 	adj_def: Metric)
//   case class Game(
//     opponent: TeamSeasonId,
//     won: Boolean,
//     rank: Int,
//     opp_rank: Int,
//     location_type: Game.LocationType.Value,
//     tier: Game.PrimaryTier.Value,
//     secondary_tiers: Set[Game.SecondaryTier.Value]
//   )
//
