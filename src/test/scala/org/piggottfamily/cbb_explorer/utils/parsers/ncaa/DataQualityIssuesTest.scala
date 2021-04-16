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
import org.joda.time.DateTime

import me.xdrop.fuzzywuzzy.FuzzySearch

object DataQualityIssuesTest extends TestSuite {

  val tests = Tests {
    "DataQualityIssues" - {

      val not_trivial_resolves = legacy_misspellings.flatMap {
        case (maybe_team_info, misspelling_map) =>
          misspelling_map.map {
            case (bad_name, good_name) =>
              //println("!!! " + DataQualityIssues.Fixer.box_aware_compare(bad_name, good_name))
              //println(s"[$bad_name -> $good_name]: $ratios ${if (token_set_ratio < 70) "****" else ""}")
              DataQualityIssues.Fixer.box_aware_compare(bad_name, good_name)
          }
      }.filter {
        case p1: DataQualityIssues.Fixer.StrongSurnameMatch =>
          p1.score < DataQualityIssues.Fixer.min_overall_score
        case _ => true
      }

      not_trivial_resolves ==> List(
          DataQualityIssues.Fixer.NoSurnameMatch(
            "GUITY,AMAYA", Some("AMAYA"),None,
            "[FINKLEA,AMAYA] vs [GUITY,AMAYA]: Failed to find a fragment matching [GUITY], candidates=(FINKLEA,17);(AMAYA,20)"
          ),
          DataQualityIssues.Fixer.WeakSurnameMatch(
            "Osborne, John",59,
            "[Osbrone, Malik] vs [Osborne, John]: Matched [Osborne] with [Some((Osbrone,86))], but overall score was [59]"
          ),
          DataQualityIssues.Fixer.NoSurnameMatch(
            "Osborne, John",Some("John"),None,
            "[Stranger, John] vs [Osborne, John]: Failed to find a fragment matching [Osborne], candidates=(Stranger,53);(John,45)"
          ),
          DataQualityIssues.Fixer.WeakSurnameMatch(
            "Tuitele, Peanut",67,
            "[Sirena Tuitele] vs [Tuitele, Peanut]: Matched [Tuitele] with [Some((Tuitele,100))], but overall score was [67]"
          ),
          DataQualityIssues.Fixer.WeakSurnameMatch(
            "James, Onome",64,
            "[Akinbode-James, O.] vs [James, Onome]: Matched [James] with [Some((Akinbode-James,90))], but overall score was [64]"
          ),
          DataQualityIssues.Fixer.WeakSurnameMatch(
            "Pryor, DeArica",69,
            "[Dee Dee Pryor] vs [Pryor, DeArica]: Matched [Pryor] with [Some((Pryor,100))], but overall score was [69]"
          ),
          DataQualityIssues.Fixer.WeakSurnameMatch(
            "Fanord, Donalson",38,
            "[Jonathan Fanard] vs [Fanord, Donalson]: Matched [Fanord] with [Some((Fanard,83))], but overall score was [38]"
          ),
          DataQualityIssues.Fixer.NoSurnameMatch(
            "PARCHMAN,OMAR",Some("OMAR"),None,
            "[PATTERSON,OMAR] vs [PARCHMAN,OMAR]: Failed to find a fragment matching [PARCHMAN], candidates=(PATTERSON,47);(OMAR,45)"
          ),
          DataQualityIssues.Fixer.NoSurnameMatch(
            "WILSON,KOBE",None,None,
            "[10] vs [WILSON,KOBE]: Failed to find a fragment matching [WILSON], candidates=(10,0)"
          )
      )
    }
  }
  def normalize_to_box(to_norm: String, surnames: Int = 1): String = {
    to_norm.split(" ").toList match {
      case l if l.size > surnames =>
        val (l1, l2) = l.splitAt(l.size - surnames)
        s"${l2.mkString(" ")}, ${l1.mkString(" ")}"
      case l => l.mkString(" ")
    }
  }

  val legacy_misspellings: Map[Option[TeamId], Map[String, String]] = Map( // pairs - full name in box score, and also name for PbP

      Option(TeamId("Test")) -> Map(
        "Osbrone, Malik" -> "Osborne, John",
        "Stranger, John" -> "Osborne, John",
      ),

      // ACC:
      Option(TeamId("Virginia")) -> Map(
        //PbP tidy game from W 2020/21
        "Ti Stojsavlevic" -> normalize_to_box("Ti Stojsavljevic"),
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
        "Nerea Hermosa Monreal" -> normalize_to_box("Nerea Hermosa"),
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

      // Remove the Cumberlands since they are not misspellings, they are
      // just to make my life easier for dedups
      // (see DataQualityIssues.misspellings for more details)

      Option(TeamId("East Carolina")) -> Map(
        // PbP error
        "BARUIT,BITUMBA" -> "BARUTI,BITUMBA",
        //Box/PBP remove 2nd name
        "Doumbia, Ibrahim Famouke" -> "Doumbia, Ibrahim",
        "Ibrahim Famouke Doumbia" -> normalize_to_box("Ibrahim Doumbia")
      ),

      Option(TeamId("UCF")) -> Map(
        // PbP error W 2018/19
        "Korneila Wright" -> normalize_to_box("Kay Kay Wright"),
        "WRIGHT,KORNEILA" -> "WRIGHT, KAY KAY",
      ),

      // America East
      Option(TeamId("Binghamton")) -> Map(
        // PbP error 2020/21
        "Ocheneyole Akuwovo" -> normalize_to_box("Ogheneyole Akuwovo"),
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
        "Dee Dee Pryor" -> normalize_to_box("DeArica Pryor"),
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
        "Lauren Jense" -> normalize_to_box("Lauren Jensen"),
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
        "B.J. Greenlee" -> normalize_to_box("Bryan Greenlee")
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
        "Sherwyn Devonish" -> normalize_to_box("Sherwyn Devonish-Prince"),
        "Sherwyn Devonish-Prince Jr." -> normalize_to_box("Sherwyn Devonish-Prince"),
        "Lapri Pace" -> normalize_to_box("Lapri McCray-Pace")
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
        "LaMont Drew" -> normalize_to_box("Drew LaMont")
      ),

      // NEC

      Option(TeamId("LIU")) -> Map(
        // PBP name difference 2020/21
        "Anthony Cabala" -> normalize_to_box("Anthony Kabala")
      ),
      Option(TeamId("Sacred Heart")) -> Map(
        "Quest Harrist" -> normalize_to_box("Quest Harris")
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
        "Sirena Tuitele" -> normalize_to_box("Peanut Tuitele"),
        "HOLLINSHED,MYA" -> "HOLLINGSHED,MYA"
      ),

      Option(TeamId("Oregon")) -> Map(
        // Was handled by generic misspellings but that doesn't work for player analysis, see below:
        "CAHVEZ,TAYLOR" -> "CHAVEZ,TAYLOR"
      ),

      // SEC:

      // Wrong in the PBP
      Option(TeamId("Arkansas")) -> Map(
        "Jordan Philips" -> normalize_to_box("Jordan Phillips"),
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
        "Jonathan Fanard" -> normalize_to_box("Donalson Fanord"),
        "WALDON,QUOIREN" -> "WALDEN,QUOIREN"
      )
    )
}
