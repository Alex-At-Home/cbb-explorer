package org.piggottfamily.cbb_explorer.controllers

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.kenpom._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.parsers.kenpom._

import cats.implicits._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

import shapeless.syntax.std.tuple._

import java.nio.file.{Path, Paths}
import scala.util.Try

// Put the object first, ran into implicit resolution issues
object StorageController {

  val default_cache_root: Path = Paths.get(System.getProperty("user.home"), ".cbb-explorer")
  val default_teams_cache: String =
    ".teams" // TODO include year for easy upload?
  val default_lineup_cache: String = s".lineups.ndjson"
  val default_player_cache: String = s".player_events.ndjson"
  val default_shot_cache: String = s".player_events.ndjson"

  val printer = Printer.noSpaces.copy(dropNullValues = true)

  /** Dependency injection */
  case class Dependencies(
      logger: LogUtils = LogUtils,
      file_manager: FileUtils = FileUtils
  )

  object JsonParserImplicits {
    import io.circe.{Decoder, Encoder, HCursor, Json}

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

    // Encode per lineup info (tupe5) into a long created by shifting ints up 10 (lets you fit 5 numbers <1024 into a the 52b mantissa of a 64b double)
    implicit val encodePlayerInfo: Encoder[LineupEventStats.PlayerTuple[Int]] =
      new Encoder[LineupEventStats.PlayerTuple[Int]] {
        final def apply(in: LineupEventStats.PlayerTuple[Int]): Json = {
          val encodedTuple = Json.fromLong(
            in.toList.zipWithIndex.foldLeft(0L) { case (acc, (v, index)) =>
              acc + ((v.toLong & 0x3ffL) << (10 * index))
            }
          )
          encodedTuple
        }
      }

    // Enums:
    implicit val tierTypeEncoder = Encoder.encodeEnumeration(Game.TierType)
    implicit val tierTypeDecoder = Decoder.decodeEnumeration(Game.TierType)
    implicit val locationTypeEncoder = Encoder.encodeEnumeration(Game.LocationType)
    implicit val locationTypeDecoder = Decoder.decodeEnumeration(Game.LocationType)

    // Ensure that we write maps in scala as arrays in JSON:
    implicit val playerMapEncoder =
      new Encoder[Map[PlayerId, PlayerSeasonSummaryStats]] {
        def apply(m: Map[PlayerId, PlayerSeasonSummaryStats]): Json = {
          m.toList.map { case (k, v) =>
            v.asJsonObject.add("id", k.asJson)
          }.asJson
        }
      }
    implicit val playerMapDecoder
        : Decoder[Map[PlayerId, PlayerSeasonSummaryStats]] =
      Decoder.decodeList[JsonObject].emap { l =>
        type XorStr[X] = Either[String, X]
        l.map { json =>
          for {
            id <- json("id").get.as[PlayerId]
            playerStats <- json.remove("id").asJson.as[PlayerSeasonSummaryStats]
          } yield id -> playerStats
        }.map(_.asInstanceOf[XorStr[(PlayerId, PlayerSeasonSummaryStats)]])
          .sequence
          .map(_.toMap)
      }

    // Key encoders for maps:
    implicit val playerIdKeyEncoder = new KeyEncoder[PlayerId] {
      override def apply(playerId: PlayerId): String = playerId.name
    }
    implicit val playerIdKeyDecoder = new KeyDecoder[PlayerId] {
      override def apply(playerId: String): Option[PlayerId] = Some(
        PlayerId(playerId)
      )
    }

    // All the anyvals
    import io.circe.generic.semiauto._
    import shapeless.Unwrapped
    implicit def encodeAnyVal[W, U](implicit
        ev: W <:< AnyVal,
        unwrapped: Unwrapped.Aux[W, U],
        encoderUnwrapped: Encoder[U]
    ): Encoder[W] =
      Encoder.instance[W](v => encoderUnwrapped(unwrapped.unwrap(v)))

    implicit def decodeAnyVal[T, U](implicit
        ev: T <:< AnyVal,
        unwrapped: Unwrapped.Aux[T, U],
        decoder: Decoder[U]
    ): Decoder[T] = Decoder.instance[T] { cursor =>
      decoder(cursor).map(value => unwrapped.wrap(value))
    }
  }
}

/** Top level business logic for parsing the different datasets */
class StorageController(
    d: StorageController.Dependencies = StorageController.Dependencies()
) {
  import StorageController._
  import StorageController.JsonParserImplicits._

  /** Store KenPom data in a NDJSON format */
  def cache_teams(
      teams: Map[TeamId, Map[Year, TeamSeason]],
      cache_root: Path = default_cache_root,
      cache_name: String = default_teams_cache
  ): Unit = {
    d.file_manager.write_lines_to_file(
      cache_root.resolve(cache_name),
      teams.values.flatMap(_.values).map(_.asJson.noSpaces)
    )
  }

  /** Retrieve KenPom data from a NDJSON format */
  def decache_teams(
      cache_root: Path = default_cache_root,
      cache_name: String = default_teams_cache
  ): Try[Map[TeamId, Map[Year, TeamSeason]]] = {

    Try(
      d.file_manager
        .read_lines_from_file(
          cache_root.resolve(cache_name)
        )
        .map { json_str =>
          decode[TeamSeason](json_str)
        }
        .collect {
          case Right(team_season) => team_season
          case Left(t)            => throw t
        }
        .groupBy {
          _.team_season.team // Map[TeamId, List[TeamSeason]]
        }
        .mapValues {
          _.groupBy(_.team_season.year).mapValues(_.head)
        }
    )
  }

  /** Stores the roster in JSON */
  def write_roster(
      roster: List[RosterEntry],
      file_path: Path
  ): Unit = {
    val roster_as_map =
      roster.map(roster => roster.player_code_id.code -> roster).toMap.asJson
    d.file_manager.write_lines_to_file(
      file_path,
      List(printer.print(roster_as_map))
    )
  }

  /** Reads stored roster maps in JSON for a single team */
  def read_roster(
      file_path: Path
  ): Map[String, RosterEntry] = {
    val json_str = d.file_manager
      .read_file(
        file_path
      )

    decode[Map[String, RosterEntry]](json_str) match {
      case Right(team_roster) => team_roster
      case Left(t)            => throw t
    }
  }

  /** Store lineups in a NDJSON format */
  def write_lineups(
      lineups: List[LineupEvent],
      file_root: Path = default_cache_root,
      file_name: String = default_lineup_cache
  ): Unit = {
    d.file_manager.write_lines_to_file(
      file_root.resolve(file_name),
      lineups.map(_.asJson.noSpaces)
      // TODO: why isn't this printer.pretty to remove nulls? It appears to be intentional but I can't figure
      // out why I'd still nulls everywhere in the lineup events
    )
  }

  /** Store player events in a NDJSON format */
  def write_player_events(
      player_events: List[PlayerEvent],
      file_root: Path = default_cache_root,
      file_name: String = default_player_cache
  ): Unit = {
    d.file_manager.write_lines_to_file(
      // (use printer.pretty to handle nulls, otherwise equivalent to _.asJson.noSpaces)
      file_root.resolve(file_name),
      player_events.map(p => printer.print(p.asJson))
    )
  }

  /** Store shot events in a NDJSON format */
  def write_shot_events(
      shot_events: List[ShotEvent],
      file_root: Path = default_cache_root,
      file_name: String = default_shot_cache
  ): Unit = {
    d.file_manager.write_lines_to_file(
      // (use printer.pretty to handle nulls, otherwise equivalent to _.asJson.noSpaces)
      file_root.resolve(file_name),
      shot_events.map(p => printer.print(p.asJson))
    )
  }
}
