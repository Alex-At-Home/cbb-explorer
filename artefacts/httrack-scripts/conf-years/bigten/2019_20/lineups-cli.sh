#!/bin/bash

#(source .lineup.env first to set up these variables)
#CRAWL_PATH=TODO
#ROOT_URL=TODO
#(to get the team navigate to https://$PBP_ROOT_URL/reports/attendance?id=17900
# pick the team, select the year, then the team id is the last bit of the URL)
YEAR=2019
CONF=bigten
array=(
'486868::Indiana'
'486929::Nebraska'
'486977::Purdue'
'486901::Maryland'
'486958::Ohio+St.'
'486722::Wisconsin'
'486909::Michigan+St.'
'486968::Penn+St.'
'486910::Michigan'
'486872::Iowa'
'486865::Illinois'
'486985::Rutgers'
'486912::Minnesota'
'486955::Northwestern'
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
    # Remove the main crawl file from the caches:
    if [ -e $CONF_CRAWL_PATH/hts-cache/old.zip ]; then
      zip -d $CONF_CRAWL_PATH/hts-cache/old.zip "$PBP_ROOT_URL/teams/$TEAMID"
    fi
    if [ -e $CONF_CRAWL_PATH/hts-cache/new.zip ]; then
      zip -d $CONF_CRAWL_PATH/hts-cache/new.zip "$PBP_ROOT_URL/teams/$TEAMID"
    fi
    httrack "$PBP_ROOT_URL/teams/$TEAMID" --continue --depth=3 --path $CONF_CRAWL_PATH --robots=0 "-*" "+$PBP_ROOT_URL/contests/*/box_score" "+$PBP_ROOT_URL/game/index/*" +"$PBP_ROOT_URL/game/box_score/*?period_no=1" +"$PBP_ROOT_URL/game/play_by_play/*"

    #Check for any errors:
    ERRS=$(grep -c 'Error:' $CONF_CRAWL_PATH/hts-log.txt)
    if [ $ERRS -gt 0 ]; then
      echo "************ ERRORS $CONF_CRAWL_PATH/hts-log.txt"
      exit -1
    fi
done
