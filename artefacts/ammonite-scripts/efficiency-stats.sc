val year = Year(2019)
val val eff_root = Path("/path/to/eff.com")

// Create a file containing a year's data
val eff = kenpom_parser_controller.build_teams(eff_root, year)

storage_controller.cache_teams(eff) //(stores the data in XXX)
