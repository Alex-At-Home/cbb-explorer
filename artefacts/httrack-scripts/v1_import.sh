import_data_v1 () {
   YEAR=$1
   shift
   CONF=$1
   shift
   array=("$@")

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

      # Support for new game-based filtering (e.g. only teams with games):
      if [ "$GAME_BASED_FILTER_FILE" != "" ]; then
         #echo "Using [$GAME_BASED_FILTER_FILE] to check if $TEAMID has games"
         if ! grep -q -F "_${TEAMID}_" "$GAME_BASED_FILTER_FILE"; then
            echo "Skipping $FULLTEAMID, not in [$GAME_BASED_FILTER_FILE]"
            continue
         fi
         #echo "Processing $FULLTEAMID"
      fi

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
      # Old format:
      #httrack "$PBP_ROOT_URL/team/$FULLTEAMID" --continue --depth=3 --path $CONF_CRAWL_PATH --robots=0 "-*" "+$PBP_ROOT_URL/contests/*/box_score" "+$PBP_ROOT_URL/team/$SUBTEAMID/roster/$YEARID" "+$PBP_ROOT_URL/game/index/*" +"$PBP_ROOT_URL/game/box_score/*?period_no=1" +"$PBP_ROOT_URL/game/play_by_play/*"
      # New format:
      httrack "$PBP_ROOT_URL/team/$FULLTEAMID" -c1 --continue --depth=3 --path $CONF_CRAWL_PATH --robots=0 "-*"  "+$PBP_ROOT_URL/teams/*/roster" "+$PBP_ROOT_URL/contests/*/box_score" "+$PBP_ROOT_URL/contests/*/play_by_play" "+$PBP_ROOT_URL/contests/*/individual_stats"

      #Check for any errors:
      ERRS=$(grep -c 'Error:' $CONF_CRAWL_PATH/hts-log.txt)
      if [ $ERRS -gt 0 ]; then
         echo "************ ERRORS $CONF_CRAWL_PATH/hts-log.txt"
         exit -1
      fi   
   done
}