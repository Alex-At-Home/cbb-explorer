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

      "Fixer.box_aware_compare" -  {

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

        //println(not_trivial_resolves.mkString("\n\n"))

        not_trivial_resolves ==> List(
            DataQualityIssues.Fixer.NoSurnameMatch(
              "GUITY,AMAYA", Some("amaya"),None,
              "[finklea,amaya] vs [guity,amaya]: Failed to find a fragment matching [guity], candidates=(finklea,17);(amaya,20)"
            ),
            DataQualityIssues.Fixer.WeakSurnameMatch(
              "Tuitele, Peanut",67,
              "[sirena tuitele] vs [tuitele, peanut]: Matched [tuitele] with [Some((tuitele,100))], but overall score was [67]"
            ),
            DataQualityIssues.Fixer.WeakSurnameMatch(
              "Osborne, John",59,
              "[osbrone, malik] vs [osborne, john]: Matched [osborne] with [Some((osbrone,86))], but overall score was [59]"
            ),
            DataQualityIssues.Fixer.NoSurnameMatch(
              "Osborne, John",Some("john"),None,
              "[stranger, john] vs [osborne, john]: Failed to find a fragment matching [osborne], candidates=(stranger,53);(john,45)"
            ),
            DataQualityIssues.Fixer.NoSurnameMatch(
              "Khalil, Ali, Jr.",Some("ali"),None,
              "[ali] vs [khalil, ali, jr.]: Failed to find a fragment matching [khalil], candidates=(ali,90)"
            ),
            DataQualityIssues.Fixer.WeakSurnameMatch(
              "James, Onome",64,
              "[akinbode-james, o.] vs [james, onome]: Matched [james] with [Some((akinbode-james,90))], but overall score was [64]"
            ),
            DataQualityIssues.Fixer.WeakSurnameMatch(
              "Pryor, DeArica",69,
              "[dee dee pryor] vs [pryor, dearica]: Matched [pryor] with [Some((pryor,100))], but overall score was [69]"
            ),
            DataQualityIssues.Fixer.WeakSurnameMatch(
              "Fanord, Donalson",38,
              "[jonathan fanard] vs [fanord, donalson]: Matched [fanord] with [Some((fanard,83))], but overall score was [38]"
            ),
            DataQualityIssues.Fixer.NoSurnameMatch(
              "PARCHMAN,OMAR",Some("omar"),None,
              "[patterson,omar] vs [parchman,omar]: Failed to find a fragment matching [parchman], candidates=(patterson,47);(omar,45)"
            ),
            DataQualityIssues.Fixer.NoSurnameMatch(
              "WILSON,KOBE",None,None,
              "[10] vs [wilson,kobe]: Failed to find a fragment matching [wilson], candidates=(10,0)"
            )
        )
      }
      "Fixer.fuzzy_box_match" -  {

        // Works: strong match, even if there are weak matches
        TestUtils.inside(DataQualityIssues.Fixer.fuzzy_box_match(
          "sirena tuitele",
          List("Suitele, Sirena", "Tuitele, Peanut", "Guity, Amaya", "Pryor, DeArica", "Guity, Robison"),
          "test1b"
        )) {
          case Right("Suitele, Sirena") =>
        }

        // Multiple strong matches, so will error
        TestUtils.inside(DataQualityIssues.Fixer.fuzzy_box_match(
          "sirena tuitele",
          List("Suitele, Sirena", "Tuitele, Irena", "Guity, Amaya", "Pryor, DeArica", "Guity, Robison"),
          "test1b"
        )) {
          case Left(err) if err.contains("ERROR.1A") =>
        }

        // Works: single weak match (even three is a matching first name)
        TestUtils.inside(DataQualityIssues.Fixer.fuzzy_box_match(
          "sirena tuitele",
          List("Tuitele, Peanut", "Guity, Amaya", "Pryor, DeArica", "Guity, Sirena"),
          "test2b"
        )) {
          case Right("Tuitele, Peanut") =>
        }

        // Multiple weak matches, so will error
        TestUtils.inside(DataQualityIssues.Fixer.fuzzy_box_match(
          "sirena tuitele",
          List("Tuitele, Peanut", "Tuitele, Rabbit", "Guity, Amaya", "Pryor, DeArica", "Guity, Robison"),
          "test1b"
        )) {
          case Left(err) if err.contains("ERROR.2A") =>
        }

        // Works because is the only matching firstname (other surnames irrelevant)
        TestUtils.inside(DataQualityIssues.Fixer.fuzzy_box_match(
          "FINKLEA,AMAYA",
          List("Guity, Amaya", "Pryor, DeArica", "Guity, Robison"),
          "test3c"
        )) {
          case Right("Guity, Amaya") =>
        }

        // Shouldn't work because the name "Anya" should be too close
        TestUtils.inside(DataQualityIssues.Fixer.fuzzy_box_match(
          "FINKLEA,AMAYA",
          List("Guity, Amaya", "Pryor, DeArica", "Robinson, Amaya"),
          "test3a"
        )) {
          case Left(err) if err.contains("ERROR.3A") =>
        }

        // Shouldn't work because the name "Anya" should be too close
        TestUtils.inside(DataQualityIssues.Fixer.fuzzy_box_match(
          "FINKLEA,AMAYA",
          List("Guity, Amaya", "Pryor, DeArica", "Robinson, Anaya"),
          "test3b"
        )) {
          case Left(err) if err.contains("ERROR.3B") =>
        }

        // No match
        TestUtils.inside(DataQualityIssues.Fixer.fuzzy_box_match(
          "FINKLEA,ALISON",
          List("Guity, Amaya", "Pryor, DeArica", "Robinson, Anaya"),
          "test4a"
        )) {
          case Left(err) if err.contains("ERROR.4A") =>
        }
      }
    }
  }

  // Required info:

  def normalize_to_box(to_norm: String, surnames: Int = 1): String = {
    to_norm.split(" ").toList match {
      case l if l.size > surnames =>
        val (l1, l2) = l.splitAt(l.size - surnames)
        s"${l2.mkString(" ")}, ${l1.mkString(" ")}"
      case l => l.mkString(" ")
    }
  }

  val legacy_misspellings: Map[Option[TeamId], Map[String, String]] = Map( // pairs - full name in box score, and also name for PbP

      // Some hand-crafted cases:
      Option(TeamId("Test1")) -> Map(
        "Osbrone, Malik" -> "Osborne, John",
        "Stranger, John" -> "Osborne, John",
        "Ali" -> "Khalil, Ali, Jr.",
      ),
      Option(TeamId("Test2")) -> Map(
        "Ali" -> "Ali, Jr., Rahim"
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
        // PbP is wrong (2020/21)
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
