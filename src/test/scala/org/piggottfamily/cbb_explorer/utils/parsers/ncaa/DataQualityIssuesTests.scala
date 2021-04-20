package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
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

object DataQualityIssuesTests extends TestSuite {
  import DataQualityIssues._

  val schedule_html = Source.fromURL(getClass.getResource("/ncaa/test_schedule.html")).mkString

  val tests = Tests {
    "DataQualityIssuesTests" - {

      // Just check the "from" construct works on the snapshot of the data when I added it...

      TestUtils.inside(Set((
        // Mitchell brothers
        combos("Makhi", "Mitchell") ++ combos("Makhel", "Mitchell") ++
        // Hamilton brothers
        combos("Jared", "Hamilton") ++ combos("Jairus", "Hamilton") ++
        // Wisconsin team-mates, leave Jordan with Jo and Jonathan gets Jn
        //"davis, jordan", "jordan davis", "davis,jordan",
        combos("Jonathan", "Davis") ++
        // Cumberland relatives
        // These two have the same name regardless of strategy! Use misspellings to tuncate Jaev's name
        combos("Jaev", "Cumberland") ++ combos("Jarron", "Cumberland") ++
        Nil
      ):_*).map(_.toLowerCase)) {
        case s => s ==> Set(
          "mitchell, makhi", "makhi mitchell", "mitchell,makhi",
          "mitchell, makhel", "makhel mitchell", "mitchell,makhel",
          "hamilton, jared", "jared hamilton", "hamilton,jared",
          "hamilton, jairus", "jairus hamilton", "hamilton,jairus",
          // Wisconsin team-mates, leave Jordan with Jo and Jonathan gets Jn
          //"davis, jordan", "jordan davis", "davis,jordan",
          "davis, jonathan", "jonathan davis", "davis,jonathan",
          // These two have the same name regardless of strategy! Use misspellings to tuncate Jaev's name
          "cumberland, jaev", "jaev cumberland", "cumberland,jaev",
          "cumberland, jarron", "jarron cumberland", "cumberland,jarron",
        )
      }

      TestUtils.inside(Option(TeamId("Fordham")) -> Map(( //A10
        // Lots of box scores has him by this nickname, as do PbP
        alias_combos(("Josh", "Colon") -> "Navarro, Josh")
      ):_*)) {
        case m => m ==> Option(TeamId("Fordham")) -> Map( //A10
          // Lots of box scores has him by this nickname, as do PbP
          "Colon, Josh" -> "Navarro, Josh",
          "COLON,JOSH" -> "Navarro, Josh",
          "Josh Colon" -> "Navarro, Josh"
        )
      }

      TestUtils.inside(Option(TeamId("Cincinnati")) -> Map((
        // The Cumberlands have caused quite a mess!
        // Truncate Jaevin's name (sorry Jaevin!)
        Seq("CUMBERLAND,J" -> "CUMBERLAND,JARRON") ++ //(just in case!)
        alias_combos(("Jaevin", "Cumberland") -> "Cumberland, Jaev")
      ):_*)) {
        case m => m ==> Option(TeamId("Cincinnati")) -> Map(
          // The Cumberlands have caused quite a mess!
          // Truncate Jaevin's name (sorry Jaevin!)
          "CUMBERLAND,J" -> "CUMBERLAND,JARRON", //(just in case!)
          "Cumberland, Jaevin" -> "Cumberland, Jaev",
          "CUMBERLAND,JAEVIN" -> "Cumberland, Jaev",
          "Jaevin Cumberland" -> "Cumberland, Jaev"
        )
      }
    }
  }
}
