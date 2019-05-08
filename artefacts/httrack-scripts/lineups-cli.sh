CRAWL_PATH=$1
TEAMID=$2
ROOT_URL=TODO
#(to get the team navigate to https://$ROOT_URL/reports/attendance?id=17900
# pick the team, select the year, then the team id is the last bit of the URL)
httrack "http://$ROOT_URL/teams/$TEAMID" --depth=3 --path $CRAWL_PATH --robots=0 "-*" "+$ROOT_URL/game/index/*" +"$ROOT_URL/game/box_score/*?period_no=1" +"$ROOT_URL/game/play_by_play/*"
