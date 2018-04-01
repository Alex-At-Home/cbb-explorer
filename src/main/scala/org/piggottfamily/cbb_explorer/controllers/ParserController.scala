package org.piggottfamily.cbb_explorer.controllers

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.kenpom._
import ammonite.ops._

import ParserController._

/** Top level business logic for parsing the different datasets */
class ParserController(d: Dependencies = Dependencies())
{
  /** Build a map of teams */
  def build_teams(root_team_path: Path): Map[TeamId, Map[Year, TeamSeason]] = {
    val teams_or_errors = for {
      file <- ls! root_team_path |? (_.ext == "html")
      filename = file.last
      _ = Logger.info(s"Reading $filename")
      html = read! file
      team_or_error = d.team_parser.parse_team(html, filename)
      _ = team_or_error match {
        case Left(errors) =>
          Logger.info(s"Failed to parse [$filename]: [${errors.mkString(";")}]")
        case Right(ParseResponse(team, warnings)) =>
          Logger.info(s"Successfully parsed [${team.team_season}]")
          if (warnings.nonEmpty) {
            Logger.info(s"Warnings: [${warnings.mkString(";")}]")
          }
      }

    } yield team_or_error

    val team_responses = teams_or_errors.collect { case Right(team_response) => team_response }
    val errors = teams_or_errors.collect { case Left(errors) => errors }
    val warnings = team_responses.flatMap(_.warnings)
    val teams = team_responses.map(_.response)

    Logger.info(
      s"Finished parsing: successful = [${teams.size}], warnings = [${warnings.size}], errors = [${errors.size}]"
    )
    teams
      .groupBy(_.team_season.team)
      .mapValues(
        _.groupBy(_.team_season.year)
          .mapValues(_.head)
      )
  }
}

object ParserController {
  /** Logging utils */
  object Logger {
    def info(s: String): Unit = println(s"[INFO] $s")
  }

  /** Dependency injection */
  case class Dependencies(
    team_parser: TeamParser = TeamParser
  )
}
