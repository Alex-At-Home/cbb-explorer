package org.piggottfamily.cbb_explorer

import java.nio.file.{Path, Paths}
import java.io.File
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.ncaa.LineupController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.LineupErrorAnalysisUtils
import org.piggottfamily.cbb_explorer.utils.FileUtils
import scala.util.Try
import java.net.URLDecoder

object BuildRosters {

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println("""
        |--in=<<in-dir-up-to-conf-then-year>>
        |--out=<<out-dir-in-which-files-are-placed>>
        |[--team]=<<only include teams matching this string>>
        |[--unify-ids-legacy]
        """)
      System.exit(-1)
    }
    val in_dir = args.toList
      .map(_.trim)
      .filter(_.startsWith("--in="))
      .headOption
      .map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--in is needed")
      }
    val out_dir = args
      .map(_.trim)
      .filter(_.startsWith("--out="))
      .headOption
      .map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--out is needed")
      }
    val maybe_team_selector = args
      .map(_.trim)
      .filter(_.startsWith("--team="))
      .headOption
      .map(_.split("=", 2)(1))

    val unify_ids_legacy =
      args // (don't use this any more, we are using CBBD ids now)
        .map(_.trim)
        .filter(_.startsWith("--unify-ids-legacy"))
        .nonEmpty

    // Get year and then conference

    val dir_segments = in_dir.split("/").toList
    val (conference, year) = dir_segments.takeRight(2) match {
      case s1 :: s2 :: Nil if Try(s2.toInt).isSuccess => (s1, s2.toInt)
      case _ =>
        throw new Exception("--in needs to end <<path>>/:conference/:year")
    }
    val gender = conference match {
      case conf if conf.startsWith("women_") => "Women"
      case _                                 => "Men"
    }

    println("Starting cbb-explorer (BuildRosters):")

    val ncaa_lineup_controller = new LineupController()
    val storage_controller = new StorageController()

    // Iterate over directories
    val subdirs = FileUtils.list_dirs(Paths.get(in_dir))
    subdirs
      .flatMap { subdir =>
        // TODO: add some error validation
        val get_team_id =
          "([^_]*)(?:_([0-9.]+))?$".r // (from 25/26 the teamid is optional)
        subdir.getFileName.toString match {
          case get_team_id(team_name, _)
              if maybe_team_selector.forall(sel => team_name.contains(sel)) =>
            val team_dir =
              if (unify_ids_legacy) subdir.resolve("roster_crawl")
              else subdir.resolve("stats.ncaa.org")

            val maybe_team_fileid = FileUtils
              .list_files(
                team_dir.resolve(LineupController.teams_dir),
                Some("html")
              )
              .take(1)
              .map {
                _.getFileName.toString.split("[.]")(0)
              }
              .headOption
            val decoded_team_name =
              URLDecoder.decode(team_name.replace("+", " "))
            Some(
              (
                team_name,
                ncaa_lineup_controller
                  .build_roster(
                    team_dir,
                    TeamId(decoded_team_name),
                    maybe_team_fileid,
                    include_coach = true,
                    unify_ncaa_ids = unify_ids_legacy
                  )
                  ._1
              )
            )

          case get_team_id(team_name, _) =>
            println(s"Skipping unselected team with dir ${subdir.toString}")
            None

          case _ =>
            println(s"Skipping unrecognized dir ${subdir.toString}")
            None
        }
      }
      .foreach { case (team_name, (_, roster)) =>
        storage_controller.write_roster(
          roster,
          Paths
            .get(out_dir)
            .resolve(s"${gender}_$year")
            .resolve(s"$team_name.json")
        )
      }
  }
}
