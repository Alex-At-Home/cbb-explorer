package org.piggottfamily.cbb_explorer

object Main {

  def main(args: Array[String]): Unit = {
    println("Starting cbb-explorer:")

    val startup = """
      |import ammonite.ops._
      |import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
      |import org.piggottfamily.cbb_explorer.models._
      |import org.piggottfamily.cbb_explorer.models.ncaa._
      |import org.piggottfamily.cbb_explorer.models.kenpom._
      |import org.piggottfamily.cbb_explorer.controllers.kenpom.ParserController
      |import org.piggottfamily.cbb_explorer.controllers.ncaa.LineupController
      |import org.piggottfamily.cbb_explorer.controllers.StorageController
      |import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
      |
      |val kenpom_parser_controller = new ParserController()
      |val ncaa_lineup_controller = new LineupController()
      |val storage_controller = new StorageController()
    """.stripMargin

    ammonite.Main(
      predefCode = startup
    ).run()
  }
}
