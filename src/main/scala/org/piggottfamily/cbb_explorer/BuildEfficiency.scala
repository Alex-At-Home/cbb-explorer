package org.piggottfamily.cbb_explorer

import ammonite.ops._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.kenpom.ParserController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import scala.util.Try
import java.net.URLDecoder
import scala.util.matching.Regex

object BuildEfficiency {

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println("""
        |--in=<<in-dir-from-crawler>>
        |--out=<<out-dir-in-which-file-is-placed>>
        |[--team]=<<only include teams matching this regex>>
        |[--year]=<<year-extracted-default-2020>>
        """)
      System.exit(-1)
    }
    val in_dir = args.toList
      .map(_.trim).filter(_.startsWith("--in="))
      .headOption.map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--in is needed")
      }
    val out_dir = args
      .map(_.trim).filter(_.startsWith("--out="))
      .headOption.map(_.split("=", 2)(1))
      .getOrElse {
        throw new Exception("--out is needed")
      }

    val maybe_team_selector = args
      .map(_.trim).filter(_.startsWith("--team="))
      .headOption

    val team_selector_regex_str =
      maybe_team_selector.map(_.split("=", 2)(1)).getOrElse("team[a-z0-9]{4}_.*")

    val year_int = args
      .map(_.trim).filter(_.startsWith("--year="))
      .headOption.map(_.split("=", 2)(1)).map(_.toInt).getOrElse(2020)

    val eff_root = Path(in_dir)

    println("Starting cbb-explorer (BuildEfficiency):")

    val kenpom_parser_controller = new ParserController()
    val eff = kenpom_parser_controller.build_teams(
      eff_root, Year(year_int), Some(new Regex(team_selector_regex_str))
    )

    // Check the right number has been processed:
    if (maybe_team_selector.isEmpty) {
      val expected_per_year = Map(
        Year(2011) -> 346,
        Year(2012) -> 347,
        Year(2013) -> 347,
        Year(2014) -> 352,
        Year(2015) -> 351,
        Year(2015) -> 351,
        Year(2016) -> 351,
        Year(2017) -> 352,
        Year(2018) -> 351,
        Year(2019) -> 353,
        Year(2020) -> 353,
        Year(2021) -> 347,
        Year(2022) -> 358
      )
      if (eff.size != expected_per_year(Year(year_int))) {
        println(s"[ERROR] Built [${eff.size}] vs expected [${expected_per_year(Year(year_int))}]")
        System.exit(-1) //(exit with error which will stop the pipeline)
      }
    }

    val storage_controller = new StorageController()
    storage_controller.cache_teams(eff,
      cache_root = Path(out_dir),
      cache_name = s"efficiency_men_${new java.util.Date().getTime()}.ndjson"
    )
  }
}
