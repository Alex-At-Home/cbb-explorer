package org.piggottfamily.cbb_explorer

object Main extends App {

  override def main(args: Array[String]): Unit = {
    println("Starting cbb-explorer:")

    ammonite.Main(
      predefCode = """
        import org.piggottfamily.cbb_explorer.models._
        import ammonite.ops._
        import org.piggottfamily.cbb_explorer.parsers._

        val parser_controller = new ParserContoller()
      """
    ).run()
  }
}
