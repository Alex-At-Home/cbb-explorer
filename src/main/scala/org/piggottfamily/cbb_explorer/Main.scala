package org.piggottfamily.cbb_explorer

object Main {

  def main(args: Array[String]): Unit = {
    println("Starting cbb-explorer:")

    val startup = """
      |import ammonite.ops._
      |import org.piggottfamily.cbb_explorer.models._
      |import org.piggottfamily.cbb_explorer.controllers.kenpom.ParserController
      |import org.piggottfamily.cbb_explorer.controllers.ncaa.LineupController
      |
      |val kenpom_parser_controller = new ParserController()
      |val ncaa_lineup_controller = new LineupController()
    """.stripMargin

    ammonite.Main(
      predefCode = startup
    ).run()
  }
}
