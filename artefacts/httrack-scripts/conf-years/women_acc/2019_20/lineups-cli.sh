#!/bin/bash

#(source .lineup.env first to set up these variables)
#CRAWL_PATH=TODO
#ROOT_URL=TODO
#(to get the team navigate to https://$PBP_ROOT_URL/reports/attendance?id=17900
# pick the team, select the year, then the team id is the last bit of the URL)
YEAR=2019
CONF=women_acc
array=(
'484175::Notre+Dame'
'484056::Louisville'
'484150::NC+State'
'483927::Duke'
'483955::Florida+St.'
'484380::Virginia'
'483887::Clemson'
'484384::Wake+Forest'
'484082::Miami+%28FL%29'
'484204::Pittsburgh'
'484113::North+Carolina'
'484325::Syracuse'
'484378::Virginia+Tech'
'483819::Boston+College'
'483974::Georgia+Tech'
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
