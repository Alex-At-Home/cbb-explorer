package org.piggottfamily.cbb_explorer.controllers

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.kenpom._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.kenpom._

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

import StorageController._
import StorageController.JsonParserImplicits._

import ammonite.ops.Path
import scala.util.Try

/** Top level business logic for parsing the different datasets */
class StorageController(d: Dependencies = Dependencies())
{
  def cache_teams(
    teams: Map[TeamId, Map[Year, TeamSeason]],
    cache_root: Path = default_cache_root,
    cache_name: String = default_teams_cache
  ): Unit = {
    //TODO
  }
  def decache_teams(
    cache_root: Path = default_cache_root,
    cache_name: String = default_teams_cache
  ): Try[Map[TeamId, Map[Year, TeamSeason]]] = {
    //TODO
    null
  }

  /** Store lineups in a NDJSON format */
  def write_lineups(
    lineups: List[LineupEvent],
    file_root: Path = default_cache_root,
    file_name: String = default_lineup_cache
  ): Unit = {
    d.file_manager.write_lines_to_file(
      file_root / file_name, lineups.map(_.asJson.noSpaces)
    )
  }

}
object StorageController {

  val default_cache_root: Path = Path.home / ".cbb-explorer"
  val default_teams_cache: String = ".teams" //TODO include year for easy upload?
  val default_lineup_cache: String = s".lineups.ndjson"

  /** Dependency injection */
  case class Dependencies(
    logger: LogUtils = LogUtils,
    file_manager: FileUtils = FileUtils
  )

  object JsonParserImplicits {
    import io.circe.{ Decoder, Encoder, HCursor, Json }

    // Date time:
    import org.joda.time.DateTime
    implicit val encodeDateTime: Encoder[DateTime] = new Encoder[DateTime] {
      final def apply(a: DateTime): Json = Json.fromString(a.toString)
    }
    implicit val decodeDateTime: Decoder[DateTime] = new Decoder[DateTime] {
      final def apply(c: HCursor): Decoder.Result[DateTime] = for {
        dt <- c.as[String]
      } yield DateTime.parse(dt)
    }

    // All the anyvals
    import io.circe.generic.semiauto._
    import shapeless.Unwrapped
    implicit def encodeAnyVal[W, U](
          implicit ev: W <:< AnyVal,
                   unwrapped: Unwrapped.Aux[W, U],
                   encoderUnwrapped: Encoder[U]
    ): Encoder[W] = Encoder.instance[W](v => encoderUnwrapped(unwrapped.unwrap(v)))
  }
}
