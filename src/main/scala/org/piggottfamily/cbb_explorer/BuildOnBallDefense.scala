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

      // Read in team on-ball defense from file as CSV and enrich with the roster info

      val team_file = Path(in_team_file)
      val team_csv = read.lines(team_file).mkString("\n")

      case class TeamOnBallDefense(
         ncaa_team: String, rank: String, team: String, gp: String, pct_time: String, poss: String, points: String,
         ppp: String, fg_miss: String, fg_made: String, fga: String, fg_pct: String, efg_pct: String, 
         to_pct: String, ft_pct: String, sf_pct: String, pct_score: String
      )
      case class EnrichedTeamOnBallDefense(
         encoded_team: String,
         on_ball: TeamOnBallDefense,
         roster: List[RosterEntry],
         box_lineup: LineupEvent
      )

      val storage_controller = new StorageController()

      val teams_map = team_csv.asCsvReader[TeamOnBallDefense](rfc.withHeader).toList.flatMap(_.toOption).flatMap { entry =>

         val encoded_team_name = URLEncoder.encode(entry.ncaa_team, "UTF-8").replace(" ", "+")
         Try {
            storage_controller.read_roster(Path(roster_dir) / s"Men_$year" / s"$encoded_team_name.json")
         }.recoverWith {
            case error =>
               System.out.println(s"Failed to ingest [${entry.ncaa_team}][$encoded_team_name]: $error")
               Failure(error)
         }.map(roster => (roster.values, LineupEvent( //Lots of dummy fields, only "players" field is populated
            DateTime.now(), Game.LocationType.Home, 0.0, 0.0, 0.0, LineupEvent.ScoreInfo.empty, 
            TeamSeasonId(TeamId(entry.ncaa_team), Year(year)), TeamSeasonId(TeamId(entry.ncaa_team), Year(year)), LineupEvent.LineupId("none"),
            roster.values.toList.map(_.player_code_id), 
            Nil, Nil, Nil, LineupEventStats.empty, LineupEventStats.empty, None
         ))).map { case (roster, lineup) =>
            (entry.ncaa_team, EnrichedTeamOnBallDefense(encoded_team = encoded_team_name, on_ball = entry, roster = roster.toList, box_lineup = lineup))
         }.toOption
         
      }.toMap

     System.out.println(s"BuildOnBallDefense: Ingested [${teams_map.size}] teams")

      // Read in player on-ball defense from file as CSV

      val player_file = Path(in_player_file)
      val player_csv = read.lines(player_file).mkString("\n")

      case class PlayerOnBallDefense(
         ncaa_team: String, rank: String, player: String, team: String, gp: String, pct_time: String, poss: String, points: String,
         ppp: String, fg_miss: String, fg_made: String, fga: String, fg_pct: String, efg_pct: String, 
         to_pct: String, ft_pct: String, sf_pct: String, pct_score: String
      )
      case class EnrichedPlayerOnBallDefense(
         encoded_team: String,
         team: TeamOnBallDefense,
         player: PlayerOnBallDefense,
         name_to_use: String
      )

      val players_grouped_by_team = 
         player_csv.asCsvReader[PlayerOnBallDefense](rfc.withHeader).toList.flatMap(_.toOption).groupBy(_.ncaa_team)
         
      val enriched_players_grouped_by_team = players_grouped_by_team.map { case (ncaa_team, entries) =>

         val maybe_enriched_team_entry = teams_map.get(ncaa_team)

         maybe_enriched_team_entry match {

            case Some(enriched_team_entry) =>

               val tidy_ctx = LineupErrorAnalysisUtils.build_tidy_player_context(enriched_team_entry.box_lineup)
               (ncaa_team, entries.flatMap { entry =>

                  // This makes it easier to apply the fuzzy matcher
                  val name_frags = entry.player.split(" ")
                  val tidied_player_name = if (name_frags.size == 2) {
                     s"${name_frags(0)}, ${name_frags(1)}" //(matches standard roster format)
                  } else entry.player

                  val (fixed_player, _) = LineupErrorAnalysisUtils.tidy_player(tidied_player_name, tidy_ctx)

                  val maybe_fixed_player_roster_entry = enriched_team_entry.roster.find(_.player_code_id.id.name == fixed_player)

                  maybe_fixed_player_roster_entry.map { roster_entry => 
                     EnrichedPlayerOnBallDefense(enriched_team_entry.encoded_team, enriched_team_entry.on_ball, entry, s"#${roster_entry.number} ${roster_entry.player_code_id.code}")
                  }
               })

            case None => (ncaa_team, List())
         }
      }.filter(_._2.nonEmpty)

      System.out.println(s"BuildOnBallDefense: Ingested [${enriched_players_grouped_by_team.values.flatten.size}] players from [${enriched_players_grouped_by_team.size}]")

      // Now for each team write out a block of text

      enriched_players_grouped_by_team.foreach { case (ncaa_team, entries) =>

         val td = entries.head.team //(must exist by construction)
         val encoded_team_name = entries.head.encoded_team

         val team_row = s"$ncaa_team\t100%\t${td.poss}\t${td.points}\t${td.ppp}\trank\trating\t${td.fg_miss}\t${td.fg_made}\t${td.fga}\t${td.fg_pct}\t${td.efg_pct}\t${td.to_pct}\t${td.ft_pct}\t${td.sf_pct}\t${td.pct_score}"

         val entry_rows = entries.map { entry =>
            val pd = entry.player
            s"${entry.name_to_use}\t${pd.pct_time}\t${pd.poss}\t${pd.points}\t${pd.ppp}\t${pd.rank}\trating\t${pd.fg_miss}\t${pd.fg_made}\t${pd.fga}\t${pd.fg_pct}\t${pd.efg_pct}\t${pd.to_pct}\t${pd.ft_pct}\t${pd.sf_pct}\t${pd.pct_score}"
         }

         write.over(Path(out_path) / year.toString / s"${encoded_team_name}.txt", (List(team_row) ++ entry_rows).mkString("\n"))
      }
   }
}