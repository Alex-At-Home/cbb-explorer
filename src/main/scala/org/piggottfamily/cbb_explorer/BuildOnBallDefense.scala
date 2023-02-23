package org.piggottfamily.cbb_explorer

import ammonite.ops._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.controllers.kenpom.ParserController
import org.piggottfamily.cbb_explorer.controllers.StorageController
import org.piggottfamily.cbb_explorer.controllers.StorageController.JsonParserImplicits._
import scala.util.Try
import java.net.URLEncoder
import org.joda.time.DateTime
import scala.util.matching.Regex
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.TeamIdParser
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.LineupErrorAnalysisUtils
import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.ExtractorUtils
import scala.io.Source
import scala.util.{Try, Success, Failure}

import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._

object BuildOnBallDefense {

   def main(args: Array[String]): Unit = {

      // Command line args processing

      if (args.length < 4) {
         println("""
         |--in-team=<<csv-file-to-read-for-team-defense>>
         |--in-player=<<csv-file-to-read-for-player-defense>>
         |--rosters=<<json-roster-dir>>
         |--out=<<out-dir-in-which-team-filters-are-placed>
         |--year=<<year-in-which-the-season-starts>>
         """)
         System.exit(-1)
      }
      val in_team_file = args.toList
         .map(_.trim).filter(_.startsWith("--in-team="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--in-team is needed")
         }
      val in_player_file = args.toList
         .map(_.trim).filter(_.startsWith("--in-player="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--in-player is needed")
         }

      val out_path = args
         .map(_.trim).filter(_.startsWith("--out="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--out is needed")
         }
      val roster_dir = args
         .map(_.trim).filter(_.startsWith("--rosters="))
         .headOption.map(_.split("=", 2)(1))
         .getOrElse {
            throw new Exception("--rosters is needed")
         }
      val year = args
         .map(_.trim).filter(_.startsWith("--year="))
         .headOption.map(_.split("=", 2)(1))
         .map(_.toInt)
         .getOrElse {
            throw new Exception("--year is needed")
         }

      // Build team LUT

      val team_lut_str = Source.fromURL(getClass.getResource("/onball_team_lookup.csv")).mkString         
      type TeamLutEntry = (String, String)
      val team_lut: Map[String, String] = team_lut_str.asCsvReader[TeamLutEntry](rfc).toList.flatMap(_.toOption).toMap


      // Read in team on-ball defense from file as CSV and enrich with the roster info

      val team_file = Path(in_team_file)
      val team_csv = read.lines(team_file).mkString("\n")

      case class TeamOnBallDefense(
         rank: String, team: String, gp: String, pct_time: String,
         poss: String, points: String, ppp: String, fg_miss: String, fg_made: String, fga: String, fg_pct: String, efg_pct: String, 
         sq: String, sqPpp: String, sqM: String,
         to_pct: String, ft_pct: String, pct_score: String, sf_pct: String
      )

      case class EnrichedTeamOnBallDefense(
         encoded_team: String,
         on_ball: TeamOnBallDefense,
         roster: List[RosterEntry],
         box_lineup: LineupEvent
      )

      val storage_controller = new StorageController()

      val teams_map = team_csv.asCsvReader[TeamOnBallDefense](rfc.withHeader).toList.flatMap(_.toOption).flatMap { entry =>

         val ncaa_team = team_lut.get(entry.team).getOrElse(entry.team)
         val encoded_team_name = URLEncoder.encode(ncaa_team, "UTF-8").replace(" ", "+")
         Try {
            storage_controller.read_roster(Path(roster_dir) / s"Men_$year" / s"$encoded_team_name.json")
         }.recoverWith {
            case error =>
               System.out.println(s"Failed to ingest [${ncaa_team}][$encoded_team_name]: $error")
               Failure(error)
         }.map(roster => (roster.values, LineupEvent( //Lots of dummy fields, only "players" field is populated
            DateTime.now(), Game.LocationType.Home, 0.0, 0.0, 0.0, LineupEvent.ScoreInfo.empty, 
            TeamSeasonId(TeamId(ncaa_team), Year(year)), TeamSeasonId(TeamId(ncaa_team), Year(year)), LineupEvent.LineupId("none"),
            roster.values.toList.map(_.player_code_id), 
            Nil, Nil, Nil, LineupEventStats.empty, LineupEventStats.empty, None
         ))).map { case (roster, lineup) =>
            (ncaa_team, EnrichedTeamOnBallDefense(encoded_team = encoded_team_name, on_ball = entry, roster = roster.toList, box_lineup = lineup))
         }.toOption
         
      }.toMap

     System.out.println(s"BuildOnBallDefense: Ingested [${teams_map.size}] teams")

      // Read in player on-ball defense from file as CSV

      val player_file = Path(in_player_file)
      val player_csv = read.lines(player_file).mkString("\n")

      case class PlayerOnBallDefense(
         rank: String, player: String, team: String, gp: String, pct_time: String, poss: String, points: String,
         ppp: String, fg_miss: String, fg_made: String, fga: String, fg_pct: String, efg_pct: String, 
         sq: String, sqPpp: String, sqM: String,
         to_pct: String, ft_pct: String, pct_score: String, sf_pct: String
      )
      case class EnrichedPlayerOnBallDefense(
         encoded_team: String,
         team: TeamOnBallDefense,
         player: PlayerOnBallDefense,
         name_to_use: String
      )

      val players_grouped_by_team = 
         player_csv.asCsvReader[PlayerOnBallDefense](rfc.withHeader).toList.flatMap(_.toOption).groupBy { entry =>
            team_lut.get(entry.team).getOrElse(entry.team)
         }
         
      val enriched_players_grouped_by_team = players_grouped_by_team.map { case (ncaa_team, entries) =>

         val maybe_enriched_team_entry = teams_map.get(ncaa_team)

         maybe_enriched_team_entry match {

            case Some(enriched_team_entry) =>

               val tidy_ctx = LineupErrorAnalysisUtils.build_tidy_player_context(enriched_team_entry.box_lineup)
               (ncaa_team, entries.flatMap { entry => Try {
                  
                  // This makes it easier to apply the fuzzy matcher
                  val name_frags = entry.player.split(" ")
                  val tidied_player_name = if (name_frags.size == 2) {
                     s"${name_frags(0)}, ${name_frags(1)}" //(matches standard roster format)
                  } else entry.player

                  val (fixed_player, _) = LineupErrorAnalysisUtils.tidy_player(tidied_player_name, tidy_ctx)

                  val maybe_fixed_player_roster_entry = enriched_team_entry.roster.find(_.player_code_id.id.name == fixed_player)

                  maybe_fixed_player_roster_entry.map { roster_entry => 
                     EnrichedPlayerOnBallDefense(
                        enriched_team_entry.encoded_team, enriched_team_entry.on_ball, entry, 
                        s"${roster_entry.player_code_id.code}"
                     )
                  }
               }.toOption.flatten.toList})

            case None => (ncaa_team, List())
         }
      }.filter(_._2.nonEmpty)

      System.out.println(s"BuildOnBallDefense: Ingested [${enriched_players_grouped_by_team.values.flatten.size}] players from [${enriched_players_grouped_by_team.size}]")

      // Now for each team write out a block of text

      enriched_players_grouped_by_team.foreach { case (ncaa_team, entries) =>

         val td = entries.head.team //(must exist by construction)
         val encoded_team_name = entries.head.encoded_team

         val team_row = s"${td.rank},Team,Team,${td.gp},100%,${td.poss},${td.points},${td.ppp},${td.fg_miss},${td.fg_made},${td.fga},${td.fg_pct},${td.efg_pct},${td.to_pct},${td.ft_pct},${td.pct_score},${td.sf_pct}"

         val entry_rows = entries.map { entry =>
            val pd = entry.player
            s"${pd.rank},${entry.name_to_use},${pd.team},${pd.gp},${pd.pct_time},${pd.poss},${pd.points},${pd.ppp},${pd.fg_miss},${pd.fg_made},${pd.fga},${pd.fg_pct},${pd.efg_pct},${pd.to_pct},${pd.ft_pct},${pd.pct_score},${pd.sf_pct}"
         }

         write.over(Path(out_path) / year.toString / s"${encoded_team_name}.txt", (List(team_row) ++ entry_rows).mkString("\n"))
      }
   }
}