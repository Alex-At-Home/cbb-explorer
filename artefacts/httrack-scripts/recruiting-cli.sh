#REC_CRAWL_PATH=$1
#REC_ROOT_URL=$2
httrack "https://$REC_ROOT_URL/Season/2018-Basketball/CompositeTeamRankings" --depth 9 --path $REC_CRAWL_PATH/classes --robots=0 "-*" "+$REC_ROOT_URL/Season/*-Basketball/CompositeTeamRankings"  "-$REC_ROOT_URL/Season/*-Basketball/CompositeTeamRankings?*" "+$REC_ROOT_URL/Season/*-Basketball/CompositeTeamRankings*Page*"   -v
httrack "https://$REC_ROOT_URL/Season/2018-Basketball/CompositeTeamRankings" --depth 3 --path $REC_CRAWL_PATH/teams --robots=0 "-*" "+$REC_ROOT_URL/Season/*-Basketball/CompositeTeamRankings"  "-$REC_ROOT_URL/Season/*-Basketball/CompositeTeamRankings?*" "+$REC_ROOT_URL/college/*/Season/*-Basketball/Commits" "-$REC_ROOT_URL/college/*/Season/*-Basketball/Commits?*"  -v
