package org.piggottfamily.cbb_explorer.controllers

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.kenpom._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.kenpom._

import cats.implicits._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

import ammonite.ops.Path
import scala.util.Try

// Put the object first, ran into implicit resolution issues
object StorageController {

  val default_cache_root: Path = Path.home / ".cbb-explorer"
  val default_teams_cache: String = ".teams" //TODO include year for easy upload?
  val default_lineup_cache: String = s".lineups.ndjson"
  val default_player_cache: String = s".player_events.ndjson"
  
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

    // Enums:
    implicit val tierTypeEncoder = Encoder.enumEncoder(Game.TierType)
    implicit val tierTypeDecoder = Decoder.enumDecoder(Game.TierType)
    implicit val locationTypeEncoder = Encoder.enumEncoder(Game.LocationType)
    implicit val locationTypeDecoder = Decoder.enumDecoder(Game.LocationType)

    // Ensure that we write maps in scala as arrays in JSON:
    implicit val playerMapEncoder = new Encoder[Map[PlayerId, PlayerSeasonSummaryStats]] {
      def apply(m: Map[PlayerId, PlayerSeasonSummaryStats]): Json = {
        m.toList.map { case (k, v)  => v.asJsonObject.add("id", k.asJson) }.asJson
      }
    }
    implicit val playerMapDecoder: Decoder[Map[PlayerId, PlayerSeasonSummaryStats]] =
      Decoder.decodeList[JsonObject].emap { l =>
          type XorStr[X] = Either[String,X]
          l.map { json => for {
              id <- json("id").get.as[PlayerId]
              playerStats <- json.remove("id").asJson.as[PlayerSeasonSummaryStats]
            } yield id -> playerStats
          }.map(_.asInstanceOf[XorStr[(PlayerId, PlayerSeasonSummaryStats)]])
           .sequence.map(_.toMap)
      }

    // Key encoders for maps:
    implicit val playerIdKeyEncoder = new KeyEncoder[PlayerId] {
      override def apply(playerId: PlayerId): String = playerId.name
    }
    implicit val playerIdKeyDecoder = new KeyDecoder[PlayerId] {
      override def apply(playerId: String): Option[PlayerId] = Some(PlayerId(playerId))
    }

    // All the anyvals
    import io.circe.generic.semiauto._
    import shapeless.Unwrapped
    implicit def encodeAnyVal[W, U](
          implicit ev: W <:< AnyVal,
                   unwrapped: Unwrapped.Aux[W, U],
                   encoderUnwrapped: Encoder[U]
    ): Encoder[W] = Encoder.instance[W](v => encoderUnwrapped(unwrapped.unwrap(v)))

    implicit def decodeAnyVal[T, U](
      implicit
      ev: T <:< AnyVal,
      unwrapped: Unwrapped.Aux[T, U],
      decoder: Decoder[U]): Decoder[T] = Decoder.instance[T] { cursor =>
      decoder(cursor).map(value => unwrapped.wrap(value))
    }
  }
}

/** Top level business logic for parsing the different datasets */
class StorageController(d: StorageController.Dependencies = StorageController.Dependencies())
{
  import StorageController._
  import StorageController.JsonParserImplicits._

  /** Store KenPom data in a NDJSON format */
  def cache_teams(
    teams: Map[TeamId, Map[Year, TeamSeason]],
    cache_root: Path = default_cache_root,
    cache_name: String = default_teams_cache
  ): Unit = {
    d.file_manager.write_lines_to_file(
      cache_root / cache_name, teams.values.flatMap(_.values).map(_.asJson.noSpaces)
    )
  }
  /** Retrieve KenPom data from a NDJSON format */
  def decache_teams(
    cache_root: Path = default_cache_root,
    cache_name: String = default_teams_cache
  ): Try[Map[TeamId, Map[Year, TeamSeason]]] = {

   Try(d.file_manager.read_lines_from_file(
     cache_root / cache_name
   ).map { json_str =>
     decode[TeamSeason](json_str)
   }.collect {
      case Right(team_season) => team_season
      case Left(t) => throw t
   }.groupBy {
     _.team_season.team // Map[TeamId, List[TeamSeason]]
   }.mapValues {
     _.groupBy(_.team_season.year).mapValues(_.head)
   })
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

  /** Store player events in a NDJSON format */
  def write_player_events(
    player_events: List[PlayerEvent],
    file_root: Path = default_cache_root,
    file_name: String = default_player_cache
  ): Unit = {
    d.file_manager.write_lines_to_file(
      file_root / file_name, player_events.map(_.asJson.noSpaces)
    )
  }

}
