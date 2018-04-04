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
  def build_teams(root_team_path: Path, default_year: Year): Map[TeamId, Map[Year, TeamSeason]] = {
    object display_vars {
      var approx_mem_in_use = 0
      var num_files = 0
    }
    val teams_or_errors = for {
      file <- ls! root_team_path |? (_.ext == "html")
      filename = file.last
      _ = display_vars.num_files += 1
      _ = Logger.info(s"Reading [$filename]")
      team_or_error = {
        // val source = scala.io.Source.fromFile(file.toString)
        // val html = source.mkString
        // source.close
        //(keep this out of the top level or its memory is stored for the lifetime of the loop)
        val html = read! file 
        d.team_parser.parse_team(html, filename, default_year)
      }
      _ = team_or_error match {
        case Left(errors) =>
          val errors_as_str = errors.mkString(";")
          display_vars.approx_mem_in_use += errors_as_str.size
          Logger.info(s"Failed to parse [$filename]: [$errors_as_str]")
        case Right(ParseResponse(team, warnings)) =>
          display_vars.approx_mem_in_use += team.toString.size
          Logger.info(s"Successfully parsed [${team.team_season}]")
          if (warnings.nonEmpty) {
            val warnings_as_str = warnings.mkString(";")
            display_vars.approx_mem_in_use += warnings_as_str.size
            Logger.info(s"Warnings: [$warnings_as_str]")
          }
      }
      _ = Logger.info(s"(approx mem in use (KB) = [${display_vars.approx_mem_in_use/1024}], files = [${display_vars.num_files}])")
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
