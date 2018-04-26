package org.piggottfamily.cbb_explorer.utils.parsers.kenpom

import utest._
import org.piggottfamily.cbb_explorer.models._
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

object TeamParserTests extends TestSuite with TeamParser {
  import ExtractorUtils._

  def get_doc(html: String): Document = {
    val browser = JsoupBrowser()
    browser.parseString(html)
  }
  def with_doc(html: String)(test: Document => Unit): Unit = {
    val doc = get_doc(html)
    test(doc)
  }

  val tests = Tests {
    "GameParser" - {
      "parse_date" - {
        TestUtils.inside(parse_date("nonsense", Year(2018))) {
          case Left(ParseError(_, "[date]", _)) =>
        }
        TestUtils.inside(parse_date("Mon Feb 1", Year(2015))) {
          case Right(date) =>
            date.dayOfMonth.get ==> 1
            date.monthOfYear.get ==> 2
            date.year.get ==> 2015
        }
      }
      "parse_score" - {
        TestUtils.inside(parse_score("nonsense")) {
          case Left(ParseError(_, "[score]", _)) =>
        }
        TestUtils.inside(parse_score("63-55")) {
          case Left(ParseError(_, "[score]", _)) =>
        }
        TestUtils.inside(parse_score("L, 63-55")) {
          case Right(Score(55, 63)) =>
        }
        TestUtils.inside(parse_score("  W 63-55")) {
          case Right(Score(63, 55)) =>
        }
      }
      "parse_location_type" - {
        val supported_locations =
          "Home" :: "Away" :: "Neutral" :: "Semi-Home" :: "Semi-Away" :: Nil

        TestUtils.inside(parse_location_type("rubbish")) {
          case Left(ParseUtils(_, "[location_type]", _)) =>
        }
        supported_locations.foreach { location_str =>
          TestUtils.inside(parse_location_type(location_str)) {
            case Right(Game.LocationType.Home) if location_str == "Home" =>
            case Right(Game.LocationType.Away) if location_str == "Away" =>
            case Right(Game.LocationType.Neutral) if location_str == "Neutral" =>
            case Right(Game.LocationType.SemiHome) if location_str == "Semi-Home" =>
            case Right(Game.LocationType.SemiAway) if location_str == "Semi-Away" =>
          }
        }
      }
      "parse_tier" - {
        with_doc("<img/>") { doc =>
          TestUtils.inside(parse_tier(doc.root)) {
            case Left(ParseUtils(_, "[tier]")) =>
          }
        }
        with_doc("<img src='https://kenpom.com/assets/a.gif'>") { doc =>
          TestUtils.inside(parse_tier(doc.root)) {
            case Right(Game.TierType.A)
          }
        }
        with_doc("<img src='https://kenpom.com/assets/b.gif'>") { doc =>
          TestUtils.inside(parse_tier(doc.root)) {
            case Right(Game.TierType.B)
          }
        }
      }
      "parse_games" - {
        with_doc("<p>No table</p>") { doc =>
          TestUtils.inside(parse_games(doc, Year(2010), 11)) {
            case Left(List(ParseError(_, "", _))) =>
          }
          //(other tests below)
        }
        "[file_tests]" - {
          val good_html = Source.fromURL(getClass.getResource("/teamb2512010_TestTeam___.html")).mkString

          val bad_html = good_html
            .replace("150</span>", "xxx150</span>")
            .replace("https://kenpom.com/assets/a.gif", "https://kenpom.com/assets/X.gif")

          with_doc(good_html) { doc =>
            val expected = GameParserTests.expected_team_games
            TestUtils.inside(parse_games(doc, Year(2010), 11)) {
              case Right(`expected`) =>
            }
          }
          //TODO: test case where name is bad
          with_doc(bad_html) { doc =>
            TestUtils.inside(parse_games(doc, Year(2010), 11)) {
              case Left(List(
                ParseUtils("", "[ConfOppC][opp_rank]", _),
                ParseUtils("", "[Conf Opp D][tier]", _)
              )) =>
            }
          }
        }
      }
    }
  }
}
object GameParserTests {
  val expected_team_games =
    Game(
      opponent = TeamSeasonId("OpponentA"),
      date = new DateTime(2010, 11, 16, 12, 0, 0, 0), // Nov 16
      won = false,
      score = Game.Score(55, 63),
      pace = 72,
      rank = 36,
      opp_rank = 111,
      location_type =  Game.LocationType.Away,
      tier = Game.TierType.B
    ) ::
    Game(
      opponent = TeamSeasonId("Opponent B"),
      date = new DateTime(2010, 11, 20, 12, 0, 0, 0), //Nov 20
      won = true,
      score = Game.Score(76, 66),
      pace = 74,
      rank = 11,
      opp_rank = 222,
      location_type =  Game.LocationType.Home,
      tier = Game.TierType.C
    ) ::
    Game(
      opponent = TeamSeasonId("ConfOppC"),
      date = new DateTime(2011, 1, 5, 12, 0, 0, 0), //Jan 5
      won = false,
      score = Game.Score(76, 78),
      pace = 78,
      rank = 11,
      opp_rank = 150,
      location_type =  Game.LocationType.Away,
      tier = Game.TierType.C
    ) ::
    Game(
      opponent = TeamSeasonId("Conf Opp D"),
      date = new DateTime(2011, 1, 11, 12, 0, 0, 0), //Jan 11
      won = false,
      score = Game.Score(58, 74),
      pace = 70,
      rank = 11,
      opp_rank = 22,
      location_type =  Game.LocationType.Home,
      tier = Game.TierType.A
    ) ::
    Nil
}
