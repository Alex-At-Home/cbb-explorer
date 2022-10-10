#!/bin/bash
# (put = in TEAM_FILTER to make it exact match vs URL-encoded string)

#Off season mode: do nothing except keep track of transfers
export OFFSEASON_MODE="no"
if [[ "$OFFSEASON_MODE" == "yes" ]]; then
   echo "In Off-season mode, will just keep track of transfers"
   export DAILY_IMPORT="no"
   export BUILD_EFFICIENCY="no"
   export BUILD_LEADERBOARDS="no"
fi


ENV_FILE=${ENV_FILE:=$PBP_SRC_ROOT/.scripts.env}
if [ "$ENV_FILE" = "/.scripts.env" ]; then
   echo "Need an initial ENV_FILE or PBP_SRC_ROOT"
   exit -1
fi

source $ENV_FILE
cd $PBP_OUT_DIR

if [[ "$DAILY_IMPORT" == "yes" ]]; then

   echo "daily_cbb_import: [$(date)] Fix broken play-by-play files"

   for i in $(find $PBP_CRAWL_PATH -name "*.zip" | grep "/$CURR_YEAR/"); do
      j=$(unzip -l $i | grep box_score | grep -E "\s+[0-9][0-9][0-9]?\s+" | grep -o 'https:.*') && echo "$i /// $j" && zip -d $i "$j";
   done

   ############

   echo "daily_cbb_import: [$(date)] Download / parse / upload new data"
   PING="lping" DOWNLOAD="yes" PARSE="yes" UPLOAD="yes" sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
else
   echo "daily_cbb_import: [$(date)] Skipping daily import, use DAILY_IMPORT='yes' to include"
fi

# cron: Mon,Wed,Fri,Sun
echo "daily_cbb_import: [$(date)] Checking to whether recalculate efficiency policy=[$BUILD_EFFICIENCY] day=[$(date +%u)]":
if [[ "$BUILD_EFFICIENCY" == "yes" ]] || [[ "$BUILD_EFFICIENCY" == "cron" && $(date +%u) =~ [1357] ]]; then
   echo "daily_cbb_import: [$(date)] Recalculating men's efficiency stats..."
   sh $PBP_SRC_ROOT/artefacts/scripts/full_men_efficiency_import.sh

   #TODO: women's stats

   export DAILY_IMPORT="yes"
fi

cd $HOOPEXP_SRC_DIR
source .env
if [[ "$DAILY_IMPORT" == "yes" ]]; then
   # If we've made any changes so far then redeploy
   sh handle-updated-data.sh
   source .env
fi

#TODO: offseason only, first thing in the morning: download transfers
echo "daily_cbb_import: [$(date)] Checking whether to download transfers"
if [[ $(date +%H) -lt 7 ]]; then
   echo "daily_cbb_import: [$(date)] Downloading transfers"

   sh $PBP_SRC_ROOT/artefacts/scripts/build_transfer_filter.sh

   if [[ "$OFFSEASON_MODE" == "yes" ]]; then
      # redeploy since nothing else is happening in off-season mode
      sh handle-updated-data.sh
   fi
fi

# cron: before 7a EST
echo "daily_cbb_import: [$(date)] Checking whether to build leaderboards policy=[$BUILD_LEADERBOARDS] hour=[$(date +%H)]":
if [[ "$BUILD_LEADERBOARDS" == "yes" ]] || [[ "$BUILD_LEADERBOARDS" = "cron" && $(date +%H) -lt 7 ]]; then

   echo "daily_cbb_import: [$(date)] Building leaderboards"
   if [[ "$DAILY_IMPORT" == "yes" ]]; then
      echo "Waiting 3min for the app to redeploy then recalculating leaderboard stats..."
      sleep 180
   fi
   cd $HOOPEXP_SRC_DIR
   npm run build_leaderboards -- --tier=High
   npm run build_leaderboards -- --tier=Medium
   npm run build_leaderboards -- --tier=Low
   npm run build_leaderboards -- --tier=Combo
   npm run build_leaderboards -- --gender=Women --tier=High

   # Upload to GCS (and delete on success)
   gsutil cp ./public/leaderboards/lineups/*2021*.json gs://$LEADERBOARD_BUCKET/ && \
      rm -f ./public/leaderboards/lineups/*2021*.json

   # Now need to redeploy _again_ to clear the cache
   sh handle-updated-data.sh
fi

