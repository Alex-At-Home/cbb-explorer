package org.piggottfamily.cbb_explorer.controllers

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.utils._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.kenpom._

import CacheController._

import ammonite.ops.Path
import scala.util.Try

/** Top level business logic for parsing the different datasets */
class CacheController(d: Dependencies = Dependencies())
{
  def cache_teams(
    teams: Map[TeamId, Map[Year, TeamSeason]],
    cache_root: Path = Path(default_cache_root),
    cache_name: String = default_teams_cache
  ): Unit = {

  }

  def decache_teams(
    cache_root: Path = Path(default_cache_root),
    cache_name: String = default_teams_cache
  ): Try[Map[TeamId, Map[Year, TeamSeason]]] = {
    null
  }
}
object CacheController {

  val default_cache_root = "~/.cbb-explorer"
  val default_teams_cache = ".teams"

  /** Dependency injection */
  case class Dependencies(
    logger: LogUtils = LogUtils,
    file_manager: FileUtils = FileUtils
  )
}
