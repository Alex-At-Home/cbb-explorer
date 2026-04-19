package org.piggottfamily.cbb_explorer

import java.nio.file.Paths
import org.piggottfamily.cbb_explorer.utils.FileUtils
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import java.net.URLEncoder
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.LineupErrorAnalysisUtils
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.ExtractorUtils
import org.piggottfamily.cbb_explorer.utils.parsers.offseason.NbaDeclarationParser
import scala.io.Source
import scala.util.Try

import kantan.csv._
import kantan.csv.ops._

/** Builds `women_transfer.json` from a three-column CSV
  * (`name,src team,dst team`) produced from On3 women's portal HTML, plus
  * optional NBA HTML. Team strings are normalized via
  * `transfer_team_lookup_women.csv`; rosters are read from `Women_<year>/`.
  */
object BuildWomenTransferLookup {

  def main(args: Array[String]): Unit = {

    if (args.length < 4) {
      println("""
         |--in=<<csv-name,src team,dst team-(from extract_on3_wbb_transfer_csv.py)>>
         |[--in-nba=<<html-nba-file-to-read>>]
         |--rosters=<<json-roster-dir>>
         |--out=<<out-file-for-json-(typically women_transfer.json)>>
         |--year=<<year-in-which-the-season-starts>>
         """.stripMargin)
      System.exit(-1)
    }
    val in_file = args.toList
      .map(_.trim)
      .filter(_.startsWith("--in="))
      .headOption
      .map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--in is needed")
      }

    if (!in_file.endsWith(".csv")) {
      throw new Exception(
        s"BuildWomenTransferLookup: --in [$in_file] must be .csv (name,src team,dst team)"
      )
    }

    val maybe_in_nba_file = args.toList
      .map(_.trim)
      .filter(_.startsWith("--in-nba="))
      .headOption
      .map(_.split("=", 2)(1))

    val out_path = args
      .map(_.trim)
      .filter(_.startsWith("--out="))
      .headOption
      .map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--out is needed")
      }
    val roster_dir = args
      .map(_.trim)
      .filter(_.startsWith("--rosters="))
      .headOption
      .map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--rosters is needed")
      }
    val year = args
      .map(_.trim)
      .filter(_.startsWith("--year="))
      .headOption
      .map(_.split("=", 2)(1))
      .map(_.toInt)
      .getOrElse {
        throw new Exception("--year is needed")
      }

    val team_lut_str =
      Source.fromURL(getClass.getResource("/transfer_team_lookup_women.csv")).mkString
    type TeamLutEntry = (String, String)
    val team_lut: Map[String, String] = team_lut_str
      .asCsvReader[TeamLutEntry](rfc)
      .toList
      .flatMap(_.toOption)
      .toMap

    case class TransferInfo(
        uuid: String,
        name: String,
        team: String,
        dest: Option[String]
    )

    def normalize_team_name(in: String) = {
      val preproc_team = in.replace("State", "St.")
      ExtractorUtils.remove_diacritics(
        team_lut.getOrElse(preproc_team, preproc_team)
      )
    }

    /** On3 lists "First Last"; rosters use "Last, First". Leave already-v0 names untouched. */
    def to_roster_player_name(raw: String): String = {
      val t = raw.trim
      if (t.contains(", ")) t
      else ExtractorUtils.name_in_v0_box_format(t)
    }

    val file = Paths.get(in_file)
    val csv_body = FileUtils.read_lines_from_file(file).drop(1).mkString("\n")
    type Row = (String, String, String)

    val transfers = csv_body
      .asCsvReader[Row](rfc)
      .toList
      .flatMap(_.toOption)
      .flatMap { case (rawName, rawTeam, rawDest) =>
        val rosterName = to_roster_player_name(rawName)
        val postproc_team = normalize_team_name(rawTeam.trim)
        val rawDestTrim = rawDest.trim
        val postproc_dest =
          if (rawDestTrim.isEmpty) ""
          else normalize_team_name(rawDestTrim)

        if (postproc_team != "NOT_D1") {
          Some(
            TransferInfo(
              uuid = s"$rosterName:$postproc_team:$postproc_dest",
              name = rosterName,
              team = postproc_team,
              dest = Some(postproc_dest).filter(_ != "")
            )
          )
        } else {
          None
        }
      }
      .toList

    println(
      s"BuildWomenTransferLookup: Ingested [${transfers.size}] transfers"
    )

    val nba_transfers: List[TransferInfo] = maybe_in_nba_file
      .map { filename =>
        val nba_file = Paths.get(filename)
        val nba_html = FileUtils.read_lines_from_file(nba_file).mkString("\n")

        val nba_pairs = NbaDeclarationParser
          .get_declarations(filename, nba_html)
          .getOrElse(List())
          .map { case (name, team) =>
            val preproc_team = team.replace("State", "St.").replace("’", "'")
            TransferInfo(
              s"$name:$preproc_team",
              name,
              team_lut.getOrElse(preproc_team, preproc_team),
              Some("NBA")
            )
          }

        println(
          s"BuildWomenTransferLookup: Ingested [${nba_pairs.size}] early declarations"
        )

        nba_pairs
      }
      .getOrElse(List())

    val storage_controller = new StorageController()

    val roster_vs_team: Map[String, LineupEvent] = (nba_transfers ++ transfers)
      .map(_.team)
      .distinct
      .flatMap { team_name =>
        val encoded_team_name =
          URLEncoder.encode(team_name, "UTF-8").replace(" ", "+")
        Try {
          storage_controller.read_roster(
            Paths.get(roster_dir).resolve(s"Women_$year").resolve(s"$encoded_team_name.json")
          )
        }.recoverWith { case error =>
          System.out.println(
            s"Failed to ingest [$team_name][$encoded_team_name]: $error"
          )
          scala.util.Failure(error)
        }.map(roster =>
          (
            team_name,
            LineupEvent(
              DateTime.now(),
              Game.LocationType.Home,
              0.0,
              0.0,
              0.0,
              LineupEvent.ScoreInfo.empty,
              TeamSeasonId(TeamId(team_name), Year(year)),
              TeamSeasonId(TeamId(team_name), Year(year)),
              LineupEvent.LineupId("none"),
              roster.values.toList.map(_.player_code_id),
              Nil,
              Nil,
              Nil,
              LineupEventStats.empty,
              LineupEventStats.empty,
              None
            )
          )
        ).toOption
      }
      .toMap

    println(
      s"BuildWomenTransferLookup: Ingested [${roster_vs_team.size}] teams, [${roster_vs_team.values.flatMap(_.players).size}] players"
    )

    case class TransferToFrom(f: String, t: Option[String])

    val manual_overrides: Map[String, String] = Map()

    val transfer_codes_to_team: Map[String, List[TransferToFrom]] =
      (nba_transfers ++ transfers)
        .flatMap { transfer_entry =>
          val maybe_roster = roster_vs_team.get(transfer_entry.team)
          maybe_roster.flatMap { roster =>
            val tidy_ctx =
              LineupErrorAnalysisUtils.build_tidy_player_context(roster)
            val (fixed_player, _) = LineupErrorAnalysisUtils.tidy_player(
              transfer_entry.name,
              tidy_ctx
            )

            val maybe_fixed_player_code =
              roster.players.find(_.id.name == fixed_player).map(_.code)
            maybe_fixed_player_code match {
              case None =>
              case _ =>
            }

            maybe_fixed_player_code.map { player_code =>
              val maybe_override_key = s"$player_code/${transfer_entry.team}"

              transfer_entry.copy(
                name = player_code,
                dest = transfer_entry.dest.orElse(
                  manual_overrides.get(maybe_override_key)
                )
              )
            }
          }
        }
        .groupBy(_.name)
        .mapValues { tt => tt.map(t => TransferToFrom(f = t.team, t = t.dest)) }

    println(
      s"BuildWomenTransferLookup: successfully identified [${transfer_codes_to_team.values.flatten.size}] players"
    )

    val printer = Printer.noSpaces.copy(dropNullValues = true)
    FileUtils.write_file(
      Paths.get(out_path),
      printer.print(transfer_codes_to_team.asJson)
    )
  }
}
