// Configuration:
val root_YEAR = Path("/path/to/YEAR/stats.ncaa.org/game")
val md_team = TeamId("Maryland")

// Create a file containing a single game's data
val game_id = "4743212" //(find this from the website, eg the play_by_play/boxscore_parser page)
val md_games = ncaa_lineup_controller.build_game_lineups(root_YEAR, game_id, md_team)
// Create a file containing a years' data
val md_games = ncaa_lineup_controller.build_team_lineups(root_YEAR, md_team)

storage_controller.write_lineups(md_games.right.get._1) //(stores the data in ~/.cbb-explorer/.lineups.ndjson)
