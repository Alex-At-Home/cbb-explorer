val year = Year(2020)
val eff_root = Path("/path/to/eff.com")

// Create a file containing a year's data
val eff = kenpom_parser_controller.build_teams(eff_root, year)
// Or, to get just this year:
val eff = kenpom_parser_controller.build_teams(eff_root, year, Some("team[a-z0-9]{4}_.*".r))
// Some("team[a-z0-9]{4}YYYY_.*".r for other years (eg YYYY=2017)

storage_controller.cache_teams(eff) //(stores the data to disk)
