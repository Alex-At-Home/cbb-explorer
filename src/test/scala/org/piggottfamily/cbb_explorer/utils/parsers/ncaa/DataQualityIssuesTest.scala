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

import me.xdrop.fuzzywuzzy.FuzzySearch

object DataQualityIssuesTest extends TestSuite {

  val tests = Tests {
    "DataQualityIssues" - {

      val cannot_resolve = legacy_misspellings.map {
        case (_, misspelling_map) =>
          misspelling_map.flatMap {
            case (bad_name, good_name) =>
              val simple_ratio = FuzzySearch.ratio(bad_name, good_name)
              val partial_ratio = FuzzySearch.partialRatio(bad_name, good_name)
              val token_sort_ratio = FuzzySearch.tokenSortRatio(bad_name, good_name)
              val token_sort_partial_ratio = FuzzySearch.tokenSortPartialRatio(bad_name, good_name)
              val token_set_ratio = FuzzySearch.tokenSetRatio(bad_name, good_name)
              val token_set_partial_ratio = FuzzySearch.tokenSetPartialRatio(bad_name, good_name)
              val weighted_ratio = FuzzySearch.weightedRatio(bad_name, good_name)
              val ratios = List(
                token_set_ratio, weighted_ratio
              )
              //println(s"[$bad_name -> $good_name]: $token_set_ratio ${if (token_set_ratio < 70) "****" else ""}")
              if (token_set_ratio < 70)
                List(bad_name -> good_name)
              else
                List()
          }
      }.map(_.headOption).filter(_.nonEmpty)

      cannot_resolve ==> List(
        Some("FINKLEA,AMAYA" -> "GUITY,AMAYA"),
        Some("Akinbode-James, O." -> "James, Onome"),
        Some("Jonathan Fanard" -> "Donalson Fanord"),
        Some("PATTERSON,OMAR" -> "PARCHMAN,OMAR"),
        Some("10" -> "WILSON,KOBE")
      )
    }
  }
  val legacy_misspellings: Map[Option[TeamId], Map[String, String]] = Map( // pairs - full name in box score, and also name for PbP

      // ACC:
      Option(TeamId("Virginia")) -> Map(
        //PbP tidy game from W 2020/21
        "Ti Stojsavlevic" -> "Ti Stojsavljevic",
      ),

      Option(TeamId("Duke")) -> Map(
        //Box tidy complicated game from W 2018/9
        "Akinbode-James, O." -> "James, Onome",
      ),

      Option(TeamId("Florida St.")) -> Map(
        // PBP errors (see also generic misspellings, which doesn't work for player events
        // because it doesn't change the tidy name, only fixes the code)
        "Willliams, Patrick" -> "Williams, Patrick",
        "Osbrone, Malik" -> "Osborne, Malik"
      ),

      Option(TeamId("Georgia Tech")) -> Map(
        //PBP fix complicated game from W 2019/20
        "Nerea Hermosa Monreal" -> "Nerea Hermosa",
        // PBP 2018/9
        "DIXON,LIZ" -> "DIXON,ELIZABETH"
      ),

      Option(TeamId("Syracuse")) -> Map(
        //Box tidy complicated game from W 2019/20
        "Finklea-Guity, Amaya" -> "Guity, Amaya",
        // PBP incorrectness
        "FINKLEA,AMAYA" -> "GUITY,AMAYA"
      ),

      // American

      Option(TeamId("Cincinnati")) -> Map(
        // The Cumberlands have caused quite a mess!
        // Truncate Jaevin's name (sorry Jaevin!)
        "CUMBERLAND,J" -> "CUMBERLAND,JARRON", //(just in case!)
        "Cumberland, Jaevin" -> "Cumberland, Jaev",
        "CUMBERLAND,JAEVIN" -> "CUMBERLAND,JAEV",
        "Jaevin Cumberland" -> "Jaev Cumberland"
      ),

      Option(TeamId("East Carolina")) -> Map(
        // PbP error
        "BARUIT,BITUMBA" -> "BARUTI,BITUMBA",
        //Box/PBP remove 2nd name
        "Doumbia, Ibrahim Famouke" -> "Doumbia, Ibrahim",
        "Ibrahim Famouke Doumbia" -> "Ibrahim Doumbia"
      ),

      Option(TeamId("UCF")) -> Map(
        // PbP error W 2018/19
        "Korneila Wright" -> "Kay Kay Wright",
        "WRIGHT,KORNEILA" -> "WRIGHT, KAY KAY",
      ),

      // America East
      Option(TeamId("Binghamton")) -> Map(
        // PbP error 2020/21
        "Ocheneyole Akuwovo" -> "Ogheneyole Akuwovo",
      ),

      // A10:

      Option(TeamId("La Salle")) -> Map(
        // Wrong in the box
        "Sullivan, Key" -> "Sullivan, Cian"
      ),

      // Wrong in the PbP, box score is correct
      Option(TeamId("Davidson")) -> Map(
        "gudmunsson" -> "gudmundsson"
      ),

      // Wrong in the PbP, box score is correct
      //(sufficient for getting the box score correct)
      Option(TeamId("Saint Joseph's")) -> Map(
        "longpr??" -> "longpre"
      ),

      // Big East

      Option(TeamId("Creighton")) -> Map(
        // PbP error W 2020/21
        "Dee Dee Pryor" -> "DeArica Pryor",
      ),

      // Big South
      Option(TeamId("Charleston So.")) -> Map(
        // Wrong in the PBP, 2020/21
        "SADARIUS,BOWSER" -> "BOWSER,SADARIUS",
        "PORTER,TJ" -> "PORTER JR.,TERENCE"
      ),
      Option(TeamId("Longwood")) -> Map(
        // Wrong in the PBP, 2020/21
        "NKEREUWEM" -> "NKEREUWEM,LESLIE",
        "STEFANOVIC,LLIJA" -> "STEFANOVIC,ILIJA",
        "O'CONNER,CAMERON" -> "O'CONNOR,CAMERON"
      ),

      // B1G

      Option(TeamId("Iowa")) -> Map(
        // PbP error W 2020/21
        "Lauren Jense" -> "Lauren Jensen",
      ),

      // B12:

      Option(TeamId("Oklahoma St.")) -> Map(
        //PBP fix complicated game from W 2019/20
        "DELAPP,KASSIDY" -> "DE LAPP,KASSIDY",
        // PBP fix
        "DESOUSA,CLITAN" -> "DE SOUSA,CLITAN"
      ),

      Option(TeamId("Texas")) -> Map(
        // (wrong in box score only)
        "Ostekowski, Dylan" -> "Osetkowski, Dylan",
        //(in case it's wrong in PbP)
        "ostekowski" -> "osetkowski"
      ),

      Option(TeamId("TCU")) -> Map(
        // (wrong in box score only)
        "Ascieris, Owen" -> "Aschieris, Owen",
        //(in case it's wrong in PbP)
        "ascieris" -> "aschieris"
      ),

      // C-USA

      Option(TeamId("Fla. Atlantic")) -> Map(
        // Bring PbP in line with box (2020)
        "B.J. Greenlee" -> "Bryan Greenlee"
      ),

      Option(TeamId("Middle Tenn.")) -> Map(
        // Bring PbP in line with box (2020)
        "CRISS,JV" -> "MILLNER-CRISS,JO"
      ),

      // MEAC

      Option(TeamId("Morgan St.")) -> Map(
        // PBP and box name difference 2020/21
        "DEVONISH,SHERWYN" -> "DEVONISH-PRINCE,SHERWYN",
        "Devonish, Sherwyn" -> "Devonish-Prince, Sherwyn",
        "Devonish-Prince Jr., Sherwyn" -> "Devonish-Prince, Sherwyn",
        "Sherwyn Devonish" -> "Sherwyn Devonish-Prince",
        "Sherwyn Devonish-Prince Jr." -> "Sherwyn Devonish-Prince",
        "Lapri Pace" -> "Lapri McCray-Pace"
      ),

      Option(TeamId("South Carolina St.")) -> Map(
        // Box is wrong, unusually 2020/21
        "JR., RIDEAU" -> "RIDEAU, FLOYD",
        "RIDEAU JR." -> "FLOYD RIDEAU",
        "BULTER,RASHAMEL" -> "BUTLER,RASHAMEL"
      ),

      // MWC

      Option(TeamId("Wyoming")) -> Map(
        // PBP has this the wrong way round compared to box score 2020/21
        "LaMont Drew" -> "Drew LaMont"
      ),

      // NEC

      Option(TeamId("LIU")) -> Map(
        // PBP name difference 2020/21
        "Anthony Cabala" -> "Anthony Kabala"
      ),
      Option(TeamId("Sacred Heart")) -> Map(
        "Quest Harrist" -> "Quest Harris"
      ),

      // OVC
      Option(TeamId("Eastern Ky.")) -> Map(
        // PBP name difference 2020/21
        "CRUIKSHANK,RUSSHARD" -> "CRUICKSHANK,RUSSHARD"
      ),

      // PAC-12

      Option(TeamId("Washington")) -> Map(
        // PBP errors (W 2019/20)
        "BANBERGER,ALI" -> "BAMBERGER,ALI",
        "WALKINS,TT" -> "WATKINS,TT"
      ),

      Option(TeamId("California")) -> Map(
        // PBP errors (W 2019/20)
        "SCHIPH,EVELIEN LUTJE" -> "SCHIPHOLT,EVELIEN LUTJE"
      ),

      Option(TeamId("Colorado")) -> Map(
        // PBP errors (W 2018/19)
        "TUITELE,SIRENA" -> "TUITELE,PEANUT",
        "Sirena Tuitele" -> "Peanut Tuitele",
        "HOLLINSHED,MYA" -> "HOLLINGSHED,MYA"
      ),

      Option(TeamId("Oregon")) -> Map(
        // Was handled by generic misspellings but that doesn't work for player analysis, see below:
        "CAHVEZ,TAYLOR" -> "CHAVEZ,TAYLOR"
      ),

      // SEC:

      // Wrong in the PBP
      Option(TeamId("Arkansas")) -> Map(
        "Jordan Philips" -> "Jordan Phillips",
        "PHILIPS,JORDAN" -> "PHILLIPS,JORDAN"
      ),

      Option(TeamId("South Carolina")) -> Map(
        "HERBERT HARRIGAN,M" -> "HERBERT HARRIGAN,MIKIAH",
        "HARRIGAN,M HERBERT" -> "HERBERT HARRIGAN,MIKIAH",
      ),

      //SWAC
      Option(TeamId("Southern U.")) -> Map(
        "SHIVERS,ASHANTE" -> "SHIVERS,AHSANTE"
      ),
      Option(TeamId("Alcorn")) -> Map(
        // Wrong in the PBP, 2020/21
        "10" -> "WILSON,KOBE"
      ),
      Option(TeamId("Ark.-Pine Bluff")) -> Map(
        // Wrong in the PBP, 2020/21
        "DOOITTLE,TRAVONTA" -> "DOOLITTLE,TRAVONTA",
        "PATTERSON,OMAR" -> "PARCHMAN,OMAR",
        ",ALVIN STREDIC JR" -> "STREDIC JR, ALVIN",
        ",SHAUN DOSS JR" -> "DOSS JR, SHAUN"
      ),
      Option(TeamId("Grambling")) -> Map(
        // Wrong in the PBP, 2020/21
        "SARION,MCGEE" -> "MCGEE,SARION"
      ),
      Option(TeamId("Mississippi Val.")) -> Map(
        // Wrong in the PBP, 2020/21
        "Jonathan Fanard" -> "Donalson Fanord",
        "WALDON,QUOIREN" -> "WALDEN,QUOIREN"
      )
    )
}
