package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import utest._
import org.joda.time.DateTime
import org.piggottfamily.cbb_explorer.utils.TestUtils
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._

object LineupErrorAnalysisUtilsTests extends TestSuite {
  import LineupErrorAnalysisUtils._

  val tests = Tests {
    "LineupErrorAnalysisUtils" - {

      //TODO

      "tidy_player" - {
        //TODO: adequately test by last test of ExtractorUtils.build_partial_lineup_list
        // + see tests below: convert_from_initials, convert_from_digits
        // Also see DataQualityIssuesTest
      }
      "convert_from_initials" - {
        val failures = List("", "A", "A ", "A C Jr", "ABC", "A D")
        val initial_set = Map("AoBo" -> "name1", "RoBo" -> "name2", "RaBa" -> "name3")
        TestUtils.inside(failures.map(convert_from_initials(_, initial_set))) {
          case l if l.forall(_ == None) =>
        }
        TestUtils.inside(convert_from_initials("A B", initial_set)) {
          case Some("name1") =>
        }
        TestUtils.inside(convert_from_initials("B, A", initial_set)) {
          case Some("name1") =>
        }
        TestUtils.inside(convert_from_initials("A, B", initial_set)) {
          case None => //(wrong order)
        }
        TestUtils.inside(convert_from_initials("R B", initial_set)) {
          case None => //(multi match)
        }
        TestUtils.inside(convert_from_initials("B, R", initial_set)) {
          case None => //(multi match)
        }
      }
      "convert_from_digits" - {
        val in = List("1x", "100", "1000")
        val player_codes = List(
          LineupEvent.PlayerCodeId(code = "1", id = PlayerId("bad_name")),
          LineupEvent.PlayerCodeId(code = "1000", id = PlayerId("name1"))
        )
        TestUtils.inside(in.map(convert_from_digits(_, player_codes))) {
          case List(None, None, Some("name1")) =>
        }
      }

      "validate_lineup" - {
        val now = new DateTime()
        val all_players @ (player1 :: player2 :: player3 :: player4 :: player5 ::
          player6 :: player7 :: Nil) = List(
            "Player One", "Player Two", "Player Three",
            "Player Four", "Player Five", "Player Six", "Player Seven"
          ).map(ExtractorUtils.build_player_code(_, None))
        val all_player_set = all_players.map(_.code).toSet
        val player8 = ExtractorUtils.build_player_code("Player Eight", None)

        val valid_players = player1 :: player2 :: player3 :: player4 :: player5 :: Nil
        val too_few_players = player1 :: player2 :: player3 :: player4 :: Nil
        val unknown_player = player1 :: player2 :: player3 :: player4 :: player8 :: Nil
        val multi_bad = player8 :: valid_players

        val my_team = TeamSeasonId(TeamId("TestTeam1"), Year(2017))
        val other_team = TeamSeasonId(TeamId("TestTeam2"), Year(2017))
        val base_lineup = LineupEvent(
          date = now,
          location_type = Game.LocationType.Home,
          start_min = 0.0,
          end_min = -100.0,
          duration_mins = 0.0,
          score_info = LineupEvent.ScoreInfo.empty,
          team = my_team,
          opponent = other_team,
          lineup_id = LineupEvent.LineupId.unknown,
          players = Nil,
          players_in = Nil,
          players_out = Nil,
          raw_game_events = Nil,
          team_stats = LineupEventStats.empty,
          opponent_stats = LineupEventStats.empty
        )

        val good_lineup = base_lineup.copy(players = valid_players)
        val lineup_too_many = base_lineup.copy(players = all_players)
        val lineup_too_few = base_lineup.copy(players = too_few_players)
        val lineup_unknown_player = base_lineup.copy(players = unknown_player)
        val lineup_multi_bad = base_lineup.copy(players = multi_bad)
        val lineup_inactive = base_lineup.copy(
          players = valid_players,
          raw_game_events = List(
            LineupEvent.RawGameEvent.team("0:00,0-0,PLAYER,BAD Does Stuff", 0.0)
          )
        )

        TestUtils.inside(validate_lineup(good_lineup, base_lineup, all_player_set).toList) {
          case List() =>
        }
        TestUtils.inside(validate_lineup(lineup_too_many, base_lineup, all_player_set).toList) {
          case List(ValidationError.WrongNumberOfPlayers) =>
        }
        TestUtils.inside(validate_lineup(lineup_too_few, base_lineup, all_player_set).toList) {
          case List(ValidationError.WrongNumberOfPlayers) =>
        }
        TestUtils.inside(validate_lineup(lineup_unknown_player, base_lineup, all_player_set).toList) {
          case List(ValidationError.UnknownPlayers) =>
        }
        TestUtils.inside(validate_lineup(lineup_multi_bad, base_lineup, all_player_set).toList) {
          case List(ValidationError.WrongNumberOfPlayers, ValidationError.UnknownPlayers) =>
        }
        TestUtils.inside(validate_lineup(lineup_inactive, base_lineup, all_player_set).toList) {
          case List(ValidationError.InactivePlayers) =>
        }
      }
    }
    "LineupErrorAnalysisUtils.NameFixer" - {

      "Fixer.box_aware_compare" -  {

        val not_trivial_resolves = legacy_misspellings.flatMap {
          case (maybe_team_info, misspelling_map) =>
            misspelling_map.map {
              case (bad_name, good_name) =>
                //println("!!! " + NameFixer.box_aware_compare(bad_name, good_name))
                //println(s"[$bad_name -> $good_name]: $ratios ${if (token_set_ratio < 70) "****" else ""}")
                NameFixer.box_aware_compare(bad_name, good_name)
            }
        }.filter {
          case p1: NameFixer.StrongSurnameMatch =>
            p1.score < NameFixer.min_overall_score || p1.box_name == "Khalil, Ali, Jr."
          case _ => true
        }

        //println(not_trivial_resolves.mkString("\n\n"))

        not_trivial_resolves ==> List(
            NameFixer.NoSurnameMatch(
              "GUITY,AMAYA", Some("amaya"),None,
              "[finklea,amaya] vs [guity,amaya]: Failed to find a fragment matching [guity], candidates=(finklea,17);(amaya,20)"
            ),
            NameFixer.WeakSurnameMatch(
              "Tuitele, Peanut",67,
              "[sirena tuitele] vs [tuitele, peanut]: Matched [tuitele] with [Some((tuitele,100))], but overall score was [67]"
            ),
            NameFixer.WeakSurnameMatch(
              "Osborne, John",59,
              "[osbrone, malik] vs [osborne, john]: Matched [osborne] with [Some((osbrone,86))], but overall score was [59]"
            ),
            NameFixer.NoSurnameMatch(
              "Osborne, John",Some("john"),None,
              "[stranger, john] vs [osborne, john]: Failed to find a fragment matching [osborne], candidates=(stranger,53);(john,45)"
            ),
            NameFixer.NoSurnameMatch(
              "Khalil, Ali, Jr.",Some("ali"),None,
              "[ali] vs [khalil, ali, jr.]: Failed to find a fragment matching [khalil], candidates=(ali,90)"
            ),
            NameFixer.WeakSurnameMatch(
              "James, Onome",64,
              "[akinbode-james, o.] vs [james, onome]: Matched [james] with [Some((akinbode-james,90))], but overall score was [64]"
            ),
            NameFixer.WeakSurnameMatch(
              "Pryor, DeArica",69,
              "[dee dee pryor] vs [pryor, dearica]: Matched [pryor] with [Some((pryor,100))], but overall score was [69]"
            ),
            NameFixer.WeakSurnameMatch(
              "Fanord, Donalson",38,
              "[jonathan fanard] vs [fanord, donalson]: Matched [fanord] with [Some((fanard,83))], but overall score was [38]"
            ),
            NameFixer.NoSurnameMatch(
              "PARCHMAN,OMAR",Some("omar"),None,
              "[patterson,omar] vs [parchman,omar]: Failed to find a fragment matching [parchman], candidates=(patterson,47);(omar,45)"
            ),
            NameFixer.NoSurnameMatch(
              "WILSON,KOBE",None,None,
              "[10] vs [wilson,kobe]: Failed to find a fragment matching [wilson], candidates=(10,0)"
            )
        )
      }
      "Fixer.fuzzy_box_match" -  {

        // Works: strong match, even if there are weak matches
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "sirena tuitele",
          List("Suitele, Sirena", "Tuitele, Peanut", "Guity, Amaya", "Pryor, DeArica", "Guity, Robison"),
          "test1c"
        )) {
          case Right("Suitele, Sirena") =>
        }

        // Multiple strong matches, but clear winner
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "Jones, Mike",
          List("Jones, Bates", "Kristensen, David", "Jones, Michael", "Collins, Carter", "Brajkovic, Luka"),
          "test1b"
        )) {
          case Right("Jones, Michael") =>
        }

        // Multiple strong matches, so will error
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "sirena tuitele",
          List("Suitele, Sirena", "Tuitele, Irena", "Guity, Amaya", "Pryor, DeArica", "Guity, Robison"),
          "test1a"
        )) {
          case Left(err) if err.contains("ERROR.1A") =>
        }

        // Works: single weak match (even three is a matching first name)
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "sirena tuitele",
          List("Tuitele, Peanut", "Guity, Amaya", "Pryor, DeArica", "Guity, Sirena"),
          "test2b"
        )) {
          case Right("Tuitele, Peanut") =>
        }

        // Multiple weak matches, so will error
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "sirena tuitele",
          List("Tuitele, Peanut", "Tuitele, Rabbit", "Guity, Amaya", "Pryor, DeArica", "Guity, Robison"),
          "test1b"
        )) {
          case Left(err) if err.contains("ERROR.2A") =>
        }

        // Works because is the only matching firstname (other surnames irrelevant)
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "FINKLEA,AMAYA",
          List("Guity, Amaya", "Pryor, DeArica", "Guity, Robison"),
          "test3c"
        )) {
          // Removed 3C support
          //case Right("Guity, Amaya") =>
          case Left(err) if err.contains("ERROR.3C") =>
        }

        // Shouldn't work because the name "Anya" should be too close
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "FINKLEA,AMAYA",
          List("Guity, Amaya", "Pryor, DeArica", "Robinson, Amaya"),
          "test3a"
        )) {
          case Left(err) if err.contains("ERROR.3A") =>
        }

        // Shouldn't work because the name "Anya" should be too close
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "FINKLEA,AMAYA",
          List("Guity, Amaya", "Pryor, DeArica", "Robinson, Anaya"),
          "test3b"
        )) {
          case Left(err) if err.contains("ERROR.3B") =>
        }

        // No match
        TestUtils.inside(NameFixer.fuzzy_box_match(
          "FINKLEA,ALISON",
          List("Guity, Amaya", "Pryor, DeArica", "Robinson, Anaya"),
          "test4a"
        )) {
          case Left(err) if err.contains("ERROR.4A") =>
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
            "Hmm Jr., Royce" -> "Hamm Jr., Royce"
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
  }
}
