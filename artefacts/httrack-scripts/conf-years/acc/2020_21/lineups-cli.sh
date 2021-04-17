#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2020
CONF=acc
array=(
   '255.0/15480::Georgia+Tech'
   '688.0/15480::Syracuse'
   '234.0/15480::Florida+St.'
   '490.0/15480::NC+State'
   '193.0/15480::Duke'
   '742.0/15480::Virginia+Tech'
   '545.0/15480::Pittsburgh'
   '746.0/15480::Virginia'
   '457.0/15480::North+Carolina'
   '67.0/15480::Boston+College'
   '367.0/15480::Louisville'
   '513.0/15480::Notre+Dame'
   '749.0/15480::Wake+Forest'
   '415.0/15480::Miami+%28FL%29'
   '147.0/15480::Clemson'
)

for index in "${array[@]}" ; do
    FULLTEAMID="${index%%::*}"
    TEAMID="${FULLTEAMID%%/*}"
    SUBTEAMID="${TEAMID%%.*}"
    YEARID="${FULLTEAMID##*/}"
    TEAM_NAME="${index##*::}"
    CONF_CRAWL_PATH=$PBP_CRAWL_PATH/$CONF/$YEAR/${TEAM_NAME}_${TEAMID}

    if [ "$TEAMID_FILTER" != "" ]; then
      if [ "$TEAMID_FILTER" != "$TEAMID" ]; then
        echo "Skipping filtered $TEAM_NAME, [$TEAMID_FILTER] vs [$TEAMID]"
        continue
      fi
    fi

    echo "$FULLTEAMID ($TEAMID) - $TEAM_NAME"
    #TODO: only do this if you want to remove and recalc everything, otherwise will find deltas
    #rm -rf $CONF_CRAWL_PATH
    mkdir -p $CONF_CRAWL_PATH
    # Remove the main crawl file from the caches:
    for file in old new; do
      if [ -e $CONF_CRAWL_PATH/hts-cache/${file}.zip ]; then
        for i in $(unzip -l $CONF_CRAWL_PATH/hts-cache/${file}.zip | grep -E "/teams?/" | awk '{ print $4 }'); do
          zip -d $CONF_CRAWL_PATH/hts-cache/${file}.zip $i;
        done
      fi
    done
    httrack "$PBP_ROOT_URL/team/$FULLTEAMID" --continue --depth=3 --path $CONF_CRAWL_PATH --robots=0 "-*" "+$PBP_ROOT_URL/contests/*/box_score" "+$PBP_ROOT_URL/team/$SUBTEAMID/roster/$YEARID" "+$PBP_ROOT_URL/game/index/*" +"$PBP_ROOT_URL/game/box_score/*?period_no=1" +"$PBP_ROOT_URL/game/play_by_play/*"

    #Check for any errors:
    ERRS=$(grep -c 'Error:' $CONF_CRAWL_PATH/hts-log.txt)
    if [ $ERRS -gt 0 ]; then
      echo "************ ERRORS $CONF_CRAWL_PATH/hts-log.txt"
      exit -1
    fi
done
