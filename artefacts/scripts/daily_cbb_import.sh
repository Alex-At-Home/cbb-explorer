#!/bin/bash
# (put = in TEAM_FILTER to make it exact match vs URL-encoded string)

#(ATTN: Before starting new season / off-season)
#(also don't forget to change the code so that leaderboard stats are read locally instead of GCS)
#(also don't forget to copy stats_all_Men_${PREV_OFFSEASON_YEAR)_Preseason.json.gz into public/leaderboards/lineups/)
export SEASON_YEAR="2025"
export ACADEMIC_YEAR="2026"
export OFFSEASON_YEAR="2025"

#Off season mode: do nothing except keep track of transfers
export OFFSEASON_MODE="no"
export INSEASON_PORTAL_TRACKING="no" #(set to "yes" one portal szn starts)
export PRESEASON_LEADERBOARD_MODE="no" #(this is "no" until it settles down a bit, maybe as late as June?)
if [[ "$OFFSEASON_MODE" == "yes" ]]; then
   echo "In Off-season mode, will just keep track of transfers"
   export DAILY_IMPORT="no"
   export BUILD_EFFICIENCY="no"
   export BUILD_LEADERBOARDS="no"
fi

# Use this to rebuild for a given team instead of just getting previous days games
#export FULL_TEAM_DOWNLOAD="yes"

# For some reason launchctl "randomly" runs as "root" not the user whose domain I specified
# So we switch to alex and re-run self
# TODO take user to run as from environment
WHOAMI=$(whoami)
echo "daily_cbb_import: [$(date)] Running as [$(whoami)], user validation"
if [[ "$WHOAMI" != "alex" ]]; then
   if [[ "$WHOAMI" == "root" ]]; then
      echo "daily_cbb_import: [$(date)] Switching from root to [alex] and executing [$0]"
      su alex "$0"
      exit 0
   fi
   echo -n "daily_cbb_import: [$(date)] Need to run as [alex], not: "
   id
   exit 1
fi

# For reasons I am currently debugging, two versions of this script are being run concurrently
export LOCK_DIR=/tmp/daily_cbb_import.lockdir
echo "daily_cbb_import: [$(date)] Running as [$(whoami)], check lock file"
if ! mkdir $LOCK_DIR 2>/dev/null; then
   echo "daily_cbb_import: [$(date)] Lock file exists: [$(stat $LOCK_DIR)]"
   exit 1
fi

ENV_FILE=${ENV_FILE:=$PBP_SRC_ROOT/.scripts.env}
if [ "$ENV_FILE" = "/.scripts.env" ]; then
   echo "Need an initial ENV_FILE or PBP_SRC_ROOT"
   exit 1
fi

source $ENV_FILE
cd $PBP_OUT_DIR

source $CBB_CRAWLER_SRC_DIR/.env

# Pre-season activity
# 
# Add steps here then comment out when finished:
#

#echo "Re-download rosters"
#PING=lping DOWNLOAD=yes PARSE=no UPLOAD=no CURR_YEAR=2025 CURR_YEAR_STR=2025_26 CURR_TIME=0 CONFS=all_men sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
#PING=lping DOWNLOAD=yes PARSE=no UPLOAD=no CURR_YEAR=2025 CURR_YEAR_STR=2025_26 CURR_TIME=0 CONFS=all_women sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh

# Re-enable to pause collection (later in the off-season)
#rmdir $LOCK_DIR
#exit 1


if [[ "$DAILY_IMPORT" == "yes" ]]; then

   ############

   echo "daily_cbb_import: [$(date)] Download / parse / upload new data"
   if [ "$FULL_TEAM_DOWNLOAD" != "yes" ]; then
      GAME_BASED_FILTER=$(date -v -1d '+%m:%d:%Y')
      echo "Downloading only games from [$GAME_BASED_FILTER]"

      GAME_BASED_FILTER_YDAY=$(date -v -2d '+%m:%d:%Y')
      GAME_BASED_FILTER_YDAY_SUFFIX=$(echo "$GAME_BASED_FILTER_YDAY" | sed 's/:/_/g')
      #(only recheck yesterday's game if the state file still exists)
      if [ -f "ncaa_games_${GAME_BASED_FILTER_YDAY_SUFFIX}_men.txt.DONE" ]; then
         echo "First let's recheck games from yesterday (men)"
         CLOSE_EOF="true" USE_CURR_SCHEDULE="yes" GAME_BASED_FILTER=$GAME_BASED_FILTER_YDAY PING="lping" DOWNLOAD="yes" PARSE="yes" UPLOAD="yes" CONFS="all_men" sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
      fi
      if [ -f "ncaa_games_${GAME_BASED_FILTER_YDAY_SUFFIX}_women.txt.DONE" ]; then
         echo "First let's recheck games from yesterday (women)"
         CLOSE_EOF="true" USE_CURR_SCHEDULE="yes" GAME_BASED_FILTER=$GAME_BASED_FILTER_YDAY PING="lping" DOWNLOAD="yes" PARSE="yes" UPLOAD="yes" CONFS="all_women" sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
      fi
   else
      GAME_BASED_FILTER=
      echo "Downloading all un-downloaded games"
   fi
   echo "[$(date)] Download today's games"
   GAME_BASED_FILTER=$GAME_BASED_FILTER PING="lping" DOWNLOAD="yes" PARSE="yes" UPLOAD="yes" CONFS="_all_" sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh

   #(check if we missed any files - eg games not finished - and retry)
   if [ "$FULL_TEAM_DOWNLOAD" != "yes" ]; then
      GAME_BASED_FILTER_SUFFIX=$(echo "$GAME_BASED_FILTER" | sed 's/:/_/g')
      MEN_GAMES_DONE=$(wc -l < "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_men.txt.DONE")
      MEN_GAMES_TODO=$(wc -l < "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_men.txt" )
      WOMEN_GAMES_DONE=$(wc -l < "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_women.txt.DONE")
      WOMEN_GAMES_TODO=$(wc -l < "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_women.txt" )

      echo "Men: expected<=[${MEN_GAMES_TODO}], actual=[${MEN_GAMES_DONE}]"
      echo "Women: expected<=[${WOMEN_GAMES_TODO}], actual=[${WOMEN_GAMES_DONE}]"
      if [[ "$MEN_GAMES_DONE" -ne "$MEN_GAMES_TODO" ]] || [[ "$WOMEN_GAMES_DONE" -ne "$WOMEN_GAMES_TODO" ]]; then
         if [[ 10#$(date +%H) -lt 7 ]]; then
            echo "(See if we missed any games: wait 30mins...)"
            sleep 30m
         else
            echo "(See if we missed any games: wait 5mins...)"
            sleep 5m
         fi
      fi
      if [[ "$WOMEN_GAMES_DONE" -ne "$WOMEN_GAMES_TODO" ]]; then
         echo "[$(date)] Retry missing games (women):"
         CLOSE_EOF="true" USE_CURR_SCHEDULE="yes" GAME_BASED_FILTER=$GAME_BASED_FILTER PING="lping" DOWNLOAD="yes" PARSE="yes" UPLOAD="yes" CONFS="all_women" sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
      fi
      if [[ "$MEN_GAMES_DONE" -ne "$MEN_GAMES_TODO" ]]; then
         echo "[$(date)] Retry missing games (men):"
         CLOSE_EOF="true" USE_CURR_SCHEDULE="yes" GAME_BASED_FILTER=$GAME_BASED_FILTER PING="lping" DOWNLOAD="yes" PARSE="yes" UPLOAD="yes" CONFS="all_men" sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
      fi
   fi
   
   # Check for errors (will also alert on old errors, you have to delete import_out.txt to start again)
   cat import_out.txt| grep ERRORS > tmp_alert_file.txt
   cat import_out.txt| grep "ERROR PlaywrightCrawler" >> tmp_alert_file.txt
   if [ -s tmp_alert_file.txt ]; then
      echo "daily_cbb_import: [$(date)] ERRORS in this file, send e-mail"
      cat $PBP_SRC_ROOT/artefacts/gmail-scripts/import_errors_mail.txt tmp_alert_file.txt > tmp_alert_mail.txt
      curl --ssl-reqd \
         --url 'smtps://smtp.gmail.com:465' \
         --user "hoop.explorer@gmail.com:$HOOPEXP_GMAIL" \
         --mail-from 'hoop.explorer@gmail.com' \
         --mail-rcpt 'hoop.explorer@gmail.com' \
         --upload-file tmp_alert_mail.txt
      rm -f tmp_alert_mail.txt
   fi
   rm -f tmp_alert_file.txt
   cat import_out.txt| grep "ERROR[.]" > tmp_alert_file.txt
   if [ -s tmp_alert_file.txt ]; then
      echo "daily_cbb_import: [$(date)] WARNINGS in this file, send e-mail"
      cat $PBP_SRC_ROOT/artefacts/gmail-scripts/import_warnings_mail.txt tmp_alert_file.txt > tmp_alert_mail.txt
      curl --ssl-reqd \
         --url 'smtps://smtp.gmail.com:465' \
         --user "hoop.explorer@gmail.com:$HOOPEXP_GMAIL" \
         --mail-from 'hoop.explorer@gmail.com' \
         --mail-rcpt 'hoop.explorer@gmail.com' \
         --upload-file tmp_alert_mail.txt
      rm -f tmp_alert_mail.txt
   fi
   rm -f tmp_alert_file.txt
   # Check for upload errors:
   cat $ELASTIC_FILEBEAT_LOGS/filebeat* | grep 'ERROR' | grep -v 'elasticsearch/client.go' | grep -v 'pipeline/output.go' > tmp_alert_file.txt
   if [ -s tmp_alert_file.txt ]; then
      echo "daily_cbb_import: [$(date)] ERRORS in the filebeat logs, send e-mail"
      cat $PBP_SRC_ROOT/artefacts/gmail-scripts/filebeat_errors_mail.txt tmp_alert_file.txt > tmp_alert_mail.txt
      curl --ssl-reqd \
         --url 'smtps://smtp.gmail.com:465' \
         --user "hoop.explorer@gmail.com:$HOOPEXP_GMAIL" \
         --mail-from 'hoop.explorer@gmail.com' \
         --mail-rcpt 'hoop.explorer@gmail.com' \
         --upload-file tmp_alert_mail.txt
      rm -f tmp_alert_mail.txt
   fi
   rm -f tmp_alert_file.txt

else
   echo "daily_cbb_import: [$(date)] Skipping daily import, use DAILY_IMPORT='yes' to include"
fi

# cron: Mon,Wed,Fri,Sun
echo "daily_cbb_import: [$(date)] Checking to whether recalculate efficiency policy=[$BUILD_EFFICIENCY] day=[$(date +%u)]":
if [[ "$BUILD_EFFICIENCY" == "yes" ]] || [[ "$BUILD_EFFICIENCY" == "cron" && $(date +%u) =~ [1357] ]]; then
   if [[ "$EFF_TRIGGER_UPLOAD" != "" ]]; then
      # Download latest file from KenPom (note I have his permission to do this):
      CRAWL_PATH=~/Downloads ACADEMIC_YEAR=${ACADEMIC_YEAR} npm --prefix $PBP_CRAWL_PROJECT run kenpom_daily_download

      echo "daily_cbb_import: [$(date)] Triggering men's efficiency tracking spreadsheet..."
      sh $PBP_SRC_ROOT/artefacts/scripts/men_efficiency_import.sh
      #(Ensure ES is updated)
      sleep 10
   fi
   if [[ "$EFF_WOMEN_TRIGGER_UPLOAD" != "" ]]; then
      # Download latest file from Torvik (note I have his permission to do this):

      echo "daily_cbb_import: [$(date)] Triggering women's efficiency tracking spreadsheet..."
      sh $PBP_SRC_ROOT/artefacts/scripts/women_efficiency_import.sh
      #(Ensure ES is updated)
      sleep 10
   else
      echo "(Women's efficiency pipeline not configured)"
   fi

   export DAILY_IMPORT="yes"

fi

# All steps below this one run from this directory
cd $HOOPEXP_SRC_DIR
source .env

#Hack: during the end of the regular season also sort transfers out (always ignore NBA)
if [[ "$INSEASON_PORTAL_TRACKING" == "yes" ]]; then
   IGNORE_NBA="yes" \
      sh $PBP_SRC_ROOT/artefacts/scripts/build_transfer_filter.sh | grep -v "LineupErrorAnalysisUtils"
fi

if [[ "$DAILY_IMPORT" == "yes" ]]; then
   # If we've made any changes so far then redeploy
   sh handle-updated-data.sh
   source .env
fi

# Offseason only, first thing in the morning: download transfers, update pre-season prediction
if [[ "$OFFSEASON_MODE" == "yes" ]]; then

   echo "daily_cbb_import: [$(date)] Checking whether to download transfers (offseason leaderboard [$PRESEASON_LEADERBOARD_MODE])"
   if [[ true ]]; then
      echo "daily_cbb_import: [$(date)] Downloading transfers"


      #(IGNORE_NBA until this info starts getting published somewhere)
      IGNORE_NBA="no" \
      sh $PBP_SRC_ROOT/artefacts/scripts/build_transfer_filter.sh | grep -v "LineupErrorAnalysisUtils"

      # Now we've update the transfers, recalculate the pre-season leaderboard and update GCS with the new file
      # Note this requires moving the "players_all_Men_${year_start}_*.json" to "./public/leaderboards/lineups/"
      if [[ "$PRESEASON_LEADERBOARD_MODE" == "yes" ]]; then
         echo "daily_cbb_import: [$(date)] Updating pre-season leaderboard"      
         cp $PBP_OUT_DIR/transfers_${OFFSEASON_YEAR}.json $HOOPEXP_SRC_DIR
         BUILD_OFFSEASON_STATS_LEADERBOARD=true npm run test src/__tests__/buildOffseasonStatsLeaderboard.test.ts  -- --coverage=false
         gzip ./stats_all_Men_${OFFSEASON_YEAR}_Preseason.json
         gsutil cp ./stats_all_Men_${OFFSEASON_YEAR}_Preseason.json.gz gs://$LEADERBOARD_BUCKET/ && \
            rm -f ./stats_all_Men_${OFFSEASON_YEAR}_Preseason.json.gz && \
            rm -f $HOOPEXP_SRC_DIR/transfers_${OFFSEASON_YEAR}.json
      fi

      if [[ "$OFFSEASON_MODE" == "yes" ]]; then
         # redeploy since nothing else is happening in off-season mode
         sh handle-updated-data.sh
      fi
   fi
fi

# cron: before 9a EST
echo "daily_cbb_import: [$(date)] Checking whether to build leaderboards policy=[$BUILD_LEADERBOARDS] hour=[$(date +%H)]":
if [[ "$BUILD_LEADERBOARDS" == "yes" ]] || [[ "$BUILD_LEADERBOARDS" = "cron" && 10#$(date +%H) -lt 9 ]]; then

   echo "daily_cbb_import: [$(date)] Building leaderboards"
   if [[ "$DAILY_IMPORT" == "yes" ]]; then
      echo "Waiting 3min for the app to redeploy then recalculating leaderboard stats..."
      sleep 180
   fi
   cd $HOOPEXP_SRC_DIR
   EXTRA=--extra-data YEARS="new" sh rebuild_all_leaderboards.sh

   # compress all the files
   for i in $(ls ./public/leaderboards/lineups/*${SEASON_YEAR}_[HLMC]*.json); do
      gzip -k $i
   done
   #(note we keep the old files around so that local features that need them don't need to go to GCS)

   # Upload to GCS (and delete gzips on success) - (High/Med/Low/Combo but not Preseason)
   gsutil cp ./public/leaderboards/lineups/*${SEASON_YEAR}_[HLMC]*.json.gz gs://$LEADERBOARD_BUCKET/ && \
      rm -f ./public/leaderboards/lineups/*${SEASON_YEAR}_[HLMC]*.json.gz

   # Now need to redeploy _again_ to clear the cache
   sh handle-updated-data.sh

   # Check for errors:
   npm run build_leaderboards -- --tier=Combo --gender=Men > tmp_game_validation.txt 
   npm run build_leaderboards -- --tier=Combo --gender=Women >> tmp_game_validation.txt 
   cat tmp_game_validation.txt | grep -A 'MISMATCH ERROR' > tmp_alert_file.txt
   if [ -s tmp_alert_file.txt ]; then
      echo "daily_cbb_import: [$(date)] Game validation ERRORS, send e-mail"
      cat $PBP_SRC_ROOT/artefacts/gmail-scripts/game_validation_errors_mail.txt tmp_alert_file.txt > tmp_alert_mail.txt
      curl --ssl-reqd \
         --url 'smtps://smtp.gmail.com:465' \
         --user "hoop.explorer@gmail.com:$HOOPEXP_GMAIL" \
         --mail-from 'hoop.explorer@gmail.com' \
         --mail-rcpt 'hoop.explorer@gmail.com' \
         --upload-file tmp_alert_mail.txt
      rm -f tmp_alert_mail.txt
   fi
   rm -f tmp_game_validation.txt

   # Finally upload "extra" player data
   cd $PBP_OUT_DIR
   sh $PBP_SRC_ROOT/artefacts/beats-scripts/players-upload.sh
fi

# Clear out the lock dir
rmdir $LOCK_DIR
