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

   echo "daily_cbb_import: [$(date)] Fix broken play-by-play files TEST 1"
   #^ (error in the zip file)

   for i in $(find $PBP_CRAWL_PATH -name "*.zip" | grep "/$CURR_YEAR/"); do
      j=$(unzip -l $i | grep box_score | grep -E "\s+[0-9][0-9][0-9]?\s+" | grep -o 'https:.*') && \
         echo "FIX BROKEN1: $i /// $j" && zip -d $i "$j";
   done

   echo "daily_cbb_import: [$(date)] Fix broken play-by-play files TEST 2"
   #^ (error in the cache - this is also done in bulk_lineup_import.sh)

   for i in $(find $PBP_CRAWL_PATH -name "hts-log.txt" | grep "/$CURR_YEAR/"); do
      j=$(cat $i | grep "Error" | grep "500" | grep -o "at link https://[^ ]*" | head -n 1 | grep -o "https://[^ ]*") && \
         echo "FIX BROKEN2: $i /// $j" && zip -d $(dirname $i)/hts-cache/new.zip "$j";
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
   if [[ "$EFF_TRIGGER_UPLOAD" == "" ]]; then
      echo "daily_cbb_import: [$(date)] Recalculating full men's efficiency stats..."
      sh $PBP_SRC_ROOT/artefacts/scripts/full_men_efficiency_import.sh
   else 
      echo "daily_cbb_import: [$(date)] Triggering men's efficiency tracking spreadsheet..."
      curl "$EFF_TRIGGER_UPLOAD"
      #(Ensure ES is updated)
      sleep 10
   fi
   if [[ "$EFF_WOMEN_TRIGGER_UPLOAD" != "" ]]; then
      echo "daily_cbb_import: [$(date)] Triggering women's efficiency tracking spreadsheet..."
      sh $PBP_SRC_ROOT/artefacts/scripts/women_efficiency_import.sh
      #(Ensure ES is updated)
      sleep 10
   else
      echo "(Women's efficiency pipeline not configured)"
   fi

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
if [[ "$OFFSEASON_MODE" == "yes" ]]; then
   echo "daily_cbb_import: [$(date)] Checking whether to download transfers"
   if [[ $(date +%H) -lt 7 ]]; then
      echo "daily_cbb_import: [$(date)] Downloading transfers"

      sh $PBP_SRC_ROOT/artefacts/scripts/build_transfer_filter.sh

      if [[ "$OFFSEASON_MODE" == "yes" ]]; then
         # redeploy since nothing else is happening in off-season mode
         sh handle-updated-data.sh
      fi
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

   # compress all the files
   for i in $(ls ./public/leaderboards/lineups/*2022_[HLMC]*.json); do
      gzip $i
   done

   # Upload to GCS (and delete on success) - (High/Med/Low/Combo but not Preseason)
   gsutil cp ./public/leaderboards/lineups/*2022_[HLMC]*.json.gz gs://$LEADERBOARD_BUCKET/ && \
      rm -f ./public/leaderboards/lineups/*2022_[HLMC]*.json.gz

   # Now need to redeploy _again_ to clear the cache
   sh handle-updated-data.sh
fi

