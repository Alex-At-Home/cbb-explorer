#!/bin/bash

#(source .lineup.env first to set up these variables)
#CRAWL_PATH=TODO
#ROOT_URL=TODO
#(to get the team navigate to https://$PBP_ROOT_URL/reports/attendance?id=17900
# pick the team, select the year, then the team id is the last bit of the URL)
YEAR=2018
CONF=american
array=(
'450616::Memphis'
'450514::Cincinnati'
'450523::UConn'
'450781::Wichita+St.'
'450571::Houston'
'450727::SMU'
'450739::Temple'
'450511::UCF'
'450721::South+Fla.'
'450757::Tulsa'
'450539::East+Carolina'
'450756::Tulane'
)

#TODO add TEAM filter

for index in "${array[@]}" ; do
    TEAMID="${index%%::*}"
    TEAM_NAME="${index##*::}"
    CONF_CRAWL_PATH=$PBP_CRAWL_PATH/$CONF/$YEAR/${TEAM_NAME}_${TEAMID}

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
    httrack "http://$PBP_ROOT_URL/teams/$TEAMID" --continue --depth=3 --path $CONF_CRAWL_PATH --robots=0 "-*" "+$PBP_ROOT_URL/contests/*/box_score" "+$PBP_ROOT_URL/game/index/*" +"$PBP_ROOT_URL/game/box_score/*?period_no=1" +"$PBP_ROOT_URL/game/play_by_play/*"

    #Check for any errors:
    ERRS=$(grep -c 'Error:' $CONF_CRAWL_PATH/hts-log.txt)
    if [ $ERRS -gt 0 ]; then
      echo "************ ERRORS $CONF_CRAWL_PATH/hts-log.txt"
      exit -1
    fi
done
