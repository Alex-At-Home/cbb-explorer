package org.piggottfamily.cbb_explorer

object Main {

  def main(args: Array[String]): Unit = {
    println("Starting cbb-explorer:")

    val startup = """
      |import ammonite.ops._
      |import org.piggottfamily.cbb_explorer.models._
      |import org.piggottfamily.cbb_explorer.controllers._
      |
      |val parser_controller = new ParserController() 
    """.stripMargin

    ammonite.Main(
      predefCode = startup
    ).run()
  }
}
