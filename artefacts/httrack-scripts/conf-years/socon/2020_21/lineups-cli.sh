#!/bin/bash

#(source .lineup.env first to set up these variables)
#CRAWL_PATH=TODO
#ROOT_URL=TODO
#(to get the team navigate to https://$PBP_ROOT_URL/reports/attendance?id=XXX (couln)
# pick the team, select the year, then the team id is the last bit of the URL)
YEAR=2020
CONF=socon
array=(
   '141.0/15480::The+Citadel'
   '769.0/15480::Western+Caro.'
   '244.0/15480::Furman'
   '406.0/15480::Mercer'
   '625.0/15480::Samford'
   '741.0/15480::VMI'
   '2915.0/15480::Wofford'
   '693.0/15480::Chattanooga'
   '459.0/15480::UNC+Greensboro'
   '198.0/15480::ETSU'
)

#TODO add TEAM filter

for index in "${array[@]}" ; do
    FULLTEAMID="${index%%::*}"
    TEAMID="${FULLTEAMID%%/*}"
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
    httrack "$PBP_ROOT_URL/team/$FULLTEAMID" --continue --depth=3 --path $CONF_CRAWL_PATH --robots=0 "-*" "+$PBP_ROOT_URL/contests/*/box_score" "+$PBP_ROOT_URL/game/index/*" +"$PBP_ROOT_URL/game/box_score/*?period_no=1" +"$PBP_ROOT_URL/game/play_by_play/*"

    #Check for any errors:
    ERRS=$(grep -c 'Error:' $CONF_CRAWL_PATH/hts-log.txt)
    if [ $ERRS -gt 0 ]; then
      echo "************ ERRORS $CONF_CRAWL_PATH/hts-log.txt"
      exit -1
    fi
done
