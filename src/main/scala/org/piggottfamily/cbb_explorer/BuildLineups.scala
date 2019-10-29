package org.piggottfamily.cbb_explorer

import ammonite.ops._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.ncaa.LineupController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import scala.util.Try
import java.net.URLDecoder

object BuildLineups {

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println("""
        |--in=<<in-dir-up-to-conf-then-year>>
        |--out=<<out-dir-in-which-files-are-placed>>
        """)
      System.exit(-1)
    }
    val inDir = args.toList
      .map(_.trim).filter(_.startsWith("--in="))
      .headOption.map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--in is needed")
      }
    val outDir = args
      .map(_.trim).filter(_.startsWith("--out="))
      .headOption.map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--out is needed")
      }
    //TODO: need a filter
    //TODO: need a diff mode? Or maybe a date threshold


    // Get year and then conference

    val dirSegments = inDir.split("/").toList
    val (conference, year) = dirSegments.takeRight(2) match {
      case s1 :: s2 :: Nil if Try(s2.toInt).isSuccess => (s1, s2.toInt)
      case _ => throw new Exception("--in needs to end <<path>>/:conference/:year")
    }

    println("Starting cbb-explorer (BuildLineups):")

    val ncaa_lineup_controller = new LineupController()
    val storage_controller = new StorageController()

    // Iterate over directories
    val subdirs = ls! Path(inDir)
    val all_games = subdirs.flatMap { subdir =>
      //TODO: add some error validation
      val getTeamId = "(.*)_([0-9]+)$".r
      subdir.last match {
        case getTeamId(teamName, teamId) => //TODO: need to decode this
          val teamDir =  subdir/ "stats.ncaa.org"
          val decodedTeamName = URLDecoder.decode(teamName.replace("+", " "))
          ncaa_lineup_controller.build_team_lineups(teamDir, TeamId(decodedTeamName))
        case _ =>
          println(s"Skipping unrecognized dir ${subdir.toString}")
          List()
      }
    }
    storage_controller.write_lineups(
      lineups = all_games.toList,
      file_root = Path(outDir),
      file_name = s"${conference}_$year.ndjson"
    )
  }
}
