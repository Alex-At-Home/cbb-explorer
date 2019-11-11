import org.piggottfamily.cbb_explorer.utils.parsers.ncaa.TeamIdParser

val file = Path("/path/to/file.html")
val html_str = read.lines(file).mkString("\n")
val triples = TeamIdParser.get_team_triples(file.toString, html_str)
val conf_strings = TeamIdParser.build_lineup_cli_array(triples.right.get)

//eg:
println(conf_strings(ConferenceId("Big Ten")))
