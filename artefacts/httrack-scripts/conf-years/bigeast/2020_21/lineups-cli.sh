#!/bin/bash

#(source .lineup.env first to set up these variables)
#CRAWL_PATH=TODO
#ROOT_URL=TODO
#(to get the team navigate to https://$PBP_ROOT_URL/reports/attendance?id=17900
# pick the team, select the year, then the team id is the last bit of the URL)
YEAR=2020
CONF=bigeast
array=(
'505546::Xavier'
'505603::Butler'
'505635::Creighton'
'505796::Providence'
'505718::Marquette'
'505666::Georgetown'
'505639::DePaul'
'505830::St.+John%27s+%28NY%29'
'505523::Villanova'
'505471::Seton+Hall'
'505632::UConn'
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
