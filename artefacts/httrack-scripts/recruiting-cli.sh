CRAWL_PATH=$1
ROOT_URL=TODO
httrack "https://$ROOT_URL/Season/2018-Basketball/CompositeTeamRankings" --depth 9 --path $CRAWL_PATH/classes --robots=0 "-*" "+$ROOT_URL/Season/*-Basketball/CompositeTeamRankings"  "-$ROOT_URL/Season/*-Basketball/CompositeTeamRankings?*" "+$ROOT_URL/Season/*-Basketball/CompositeTeamRankings*Page*"   -v
httrack "https://$ROOT_URL/Season/2018-Basketball/CompositeTeamRankings" --depth 3 --path $CRAWL_PATH/teams --robots=0 "-*" "+$ROOT_URL/Season/*-Basketball/CompositeTeamRankings"  "-$ROOT_URL/Season/*-Basketball/CompositeTeamRankings?*" "+$ROOT_URL/college/*/Season/*-Basketball/Commits" "-$ROOT_URL/college/*/Season/*-Basketball/Commits?*"  -v
