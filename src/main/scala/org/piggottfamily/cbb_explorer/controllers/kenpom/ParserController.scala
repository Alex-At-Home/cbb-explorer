package org.piggottfamily.cbb_explorer.controllers.kenpom

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.utils._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.kenpom._

import ParserController._

import ammonite.ops.Path
import scala.util.matching.Regex

/** Top level business logic for parsing the different datasets */
class ParserController(d: Dependencies = Dependencies())
{
  /** Build a map of teams */
  def build_teams(
    root_team_path: Path, default_year: Year, filename_filter: Option[Regex] = Some("team.*".r)
  ): Map[TeamId, Map[Year, TeamSeason]] = {
    object display_vars {
      var approx_mem_in_use = 0
      var num_files = 0
    }
    val teams_or_errors = for {
      file <- d.file_manager.list_files(root_team_path, Some("html"))
      filename = file.last
      if filename_filter.flatMap(_.findFirstIn(filename)).isDefined
      _ = display_vars.num_files += 1
      _ = d.logger.info(s"Reading [$filename]")
      team_or_error = {
        //(keep this out of the top level or its memory is stored for the lifetime of the loop)
        val html = d.file_manager.read_file(file)
        d.team_parser.parse_team(html, filename, default_year)
      }
      _ = team_or_error match {
        case Left(errors) =>
          val errors_as_str = errors.mkString(";")
          display_vars.approx_mem_in_use += errors_as_str.size
          d.logger.info(s"Failed to parse [$filename]: [$errors_as_str]")
        case Right(ParseResponse(team, warnings)) =>
          display_vars.approx_mem_in_use += team.toString.size
          d.logger.info(s"Successfully parsed [${team.team_season}]")
          if (warnings.nonEmpty) {
            val warnings_as_str = warnings.mkString(";")
            display_vars.approx_mem_in_use += warnings_as_str.size
            d.logger.info(s"Warnings: [$warnings_as_str]")
          }
      }
      _ = d.logger.info(s"(approx mem in use (KB) = [${display_vars.approx_mem_in_use/1024}], files = [${display_vars.num_files}])")
    } yield team_or_error

    val team_responses = teams_or_errors.collect { case Right(team_response) => team_response }
    val errors = teams_or_errors.collect { case Left(errors) => errors }
    val warnings = team_responses.flatMap(_.warnings)
    val teams = team_responses.map(_.response)

    d.logger.info(
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
  /** Dependency injection */
  case class Dependencies(
    team_parser: TeamParser = TeamParser,
    logger: LogUtils = LogUtils,
    file_manager: FileUtils = FileUtils
  )
}
