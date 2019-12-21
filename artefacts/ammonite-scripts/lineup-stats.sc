// Configuration:
val root_YEAR = Path("/path/to/YEAR/stats.ncaa.org")
val md_team = TeamId("Maryland")

// Create a file containing a single game's data
val game_id = "4743212".r //(find this from the website, eg the play_by_play/boxscore_parser page)
val (md_games, lineup_errors) = ncaa_lineup_controller.build_team_lineups(root_YEAR, md_team, Some(game_id))
storage_controller.write_lineups(md_games) //(stores the data in ~/.cbb-explorer/.lineups.ndjson)

// Create a file containing a season of games' data
val (md_games, lineup_errors) = ncaa_lineup_controller.build_team_lineups(root_YEAR, md_team)
storage_controller.write_lineups(md_games) //(stores the data in ~/.cbb-explorer/.lineups.ndjson)

// For debugging parsing errors:
val neutral_games = Set()
val good_and_bad = ncaa_lineup_controller.build_game_lineups(root_YEAR, game_id.toString, md_team, neutral_games)
