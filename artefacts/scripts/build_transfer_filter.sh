#!/bin/bash
#
# Pulls the current list of transfers and formats ready for use as a filter by the player leaderboard
#

CURR_YEAR_P1=$((CURR_YEAR+1))
HTMLTAB_LOC=/Users/alex/Library/Python/3.8/bin/

$HTMLTAB_LOC/htmltab --select "table#player-rankings" "https://verbalcommits.com/transfers/$CURR_YEAR_P1" > $PBP_OUT_DIR/transfers.csv

echo "build_transfer_filter: Downloaded $(wc -l $PBP_OUT_DIR/transfers.csv) transfers"

java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" org.piggottfamily.cbb_explorer.BuildTransferLookup --in=$PBP_OUT_DIR/transfers.csv --rosters=$HOOPEXP_SRC_DIR/public/rosters --out=$PBP_OUT_DIR/current_transfers.json --year=$CURR_YEAR

if [ $(grep -c '\[' $PBP_OUT_DIR/current_transfers.json) -gt 0 ]; then
   gsutil cp $PBP_OUT_DIR/current_transfers.json gs://$LEADERBOARD_BUCKET/ 
fi