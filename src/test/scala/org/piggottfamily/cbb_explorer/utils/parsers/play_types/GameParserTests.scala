package org.piggottfamily.cbb_explorer.utils.parsers.play_types

import utest._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.play_types._
import org.piggottfamily.cbb_explorer.utils.parsers._
import org.piggottfamily.cbb_explorer.utils.TestUtils
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._
import scala.io.Source
import com.github.dwickern.macros.NameOf._
import shapeless._
import ops.hlist._
import shapeless.labelled._
import record._
import ops.record._
import syntax.singleton._
import org.joda.time.DateTime

object GameParserTests extends TestSuite with GameParser {

  val game_html = Source.fromURL(getClass.getResource("/play_types/sample_play_types.html")).mkString

  val get_doc = (str: String) => {
    val browser = JsoupBrowser()
    browser.parseString(str)
  }

  val tests = Tests {
    "GameParser" - {
      "TODO builders.team_names_finder" - {
        TestUtils.inside(get_doc(game_html)) {
          case List("", "") =>
        }
      }
    }
  }
}
