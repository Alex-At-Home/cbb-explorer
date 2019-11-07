#!/bin/bash

#(source .lineup.env first to set up these variables)
#CRAWL_PATH=TODO
#ROOT_URL=TODO
#(to get the team navigate to https://$ROOT_URL/reports/attendance?id=17900
# pick the team, select the year, then the team id is the last bit of the URL)
YEAR=2019
CONF=bigten
array=(
    '486901::Maryland'
)

#TODO add TEAM filter

for index in "${array[@]}" ; do
    TEAMID="${index%%::*}"
    TEAM_NAME="${index##*::}"
    CONF_CRAWL_PATH=$CRAWL_PATH/$CONF/$YEAR/${TEAM_NAME}_${TEAMID}

    if [ "$TEAMID_FILTER" != "" ]; then
      if [ "$TEAMID_FILTER" != "$TEAMID" ]; then
        echo "Skipping filtered $TEAM_NAME, [$TEAMID_FILTER] vs [$TEAMID]"
        continue
      fi
    fi

    echo "$TEAMID - $TEAM_NAME"
    #TODO: only do this if you want to remove and recalc everything, otherwise will find deltas
    #rm -rf $CONF_CRAWL_PATH
    mkdir -p $CONF_CRAWL_PATH
    httrack "http://$ROOT_URL/teams/$TEAMID" --depth=3 --path $CONF_CRAWL_PATH --robots=0 "-*" "+$ROOT_URL/contests/*/box_score" "+$ROOT_URL/game/index/*" +"$ROOT_URL/game/box_score/*?period_no=1" +"$ROOT_URL/game/play_by_play/*"

    #Check for any errors:
    ERRS=$(grep -c 'Error:' $CONF_CRAWL_PATH/hts-log.txt)
    if [ $ERRS -gt 0 ]; then
      echo "************ ERRORS $CONF_CRAWL_PATH/hts-log.txt"
      exit -1
    fi
done
