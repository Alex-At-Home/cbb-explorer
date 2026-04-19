#!/bin/bash
#
# Pulls the current list of transfers and formats ready for use as a filter by the player leaderboard
# (year is confusingly the first year of the most recent active season, eg 2022 == 2022/23 == 23 offseason!)
# PROCESS_ONLY=yes to skip the download
# TRANSFER=no to not copy to GS
# IGNORE_NBA=yes to ignore NBA declarations (set this when running by hand before NBA pipeline is set up)
# ie handy for debug: TRANSFER="no" PROCESS_ONLY="yes" CURR_YEAR=2022 bash /Users/alex/personal/github/cbb-explorer/artefacts/scripts/build_transfer_filter.sh
#

CURR_YEAR_P1=$((CURR_YEAR+1))

TXFER_FILE_TO_PARSE=$PBP_OUT_DIR/women_transfers.csv

NUM_TRANSFERS=$(wc -l $TXFER_FILE_TO_PARSE | awk '{ print $1 }')

echo "build_transfer_filter: [$(date)] Downloaded [$NUM_TRANSFERS] transfers"


if [[ $NUM_TRANSFERS -gt 0 ]]; then
   java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
      org.piggottfamily.cbb_explorer.BuildWomenTransferLookup --in=$TXFER_FILE_TO_PARSE \
      --rosters=$HOOPEXP_SRC_DIR/public/rosters --out=$PBP_OUT_DIR/women_transfers_$CURR_YEAR_P1.json --year=$CURR_YEAR

   if [ "$TRANSFER" != "no" ]; then
      gsutil cp $PBP_OUT_DIR/women_transfers_$CURR_YEAR_P1.json gs://$LEADERBOARD_BUCKET/ 
   fi
fi
