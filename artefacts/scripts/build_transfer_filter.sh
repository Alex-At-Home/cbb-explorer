#!/bin/bash
#
# Pulls the current list of transfers and formats ready for use as a filter by the player leaderboard
#

CURR_YEAR_P1=$((CURR_YEAR+1))
HTMLTAB_LOC=/Users/alex/Library/Python/3.8/bin/

if [ ! -f $PBP_OUT_DIR/transfers.csv.SAVED ]; then
   echo "build_transfer_filter: [$(date)] creating new empty SAVED csv"
   echo "" > $PBP_OUT_DIR/transfers.csv.SAVED
fi

curl -H 'Cache-Control: no-cache' \
   -o $PBP_OUT_DIR/transfers.html "https://verbalcommits.com/transfers/$CURR_YEAR_P1"

$HTMLTAB_LOC/htmltab --select "table#player-rankings" $PBP_OUT_DIR/transfers.html > $PBP_OUT_DIR/transfers.csv

PREVIOUS_CSV_SIZE=$(stat -f "%z" "$PBP_OUT_DIR/transfers.csv.SAVED")
TODAY_CSV_SIZE=$(stat -f "%z" "$PBP_OUT_DIR/transfers.csv")

if (( TODAY_CSV_SIZE > PREVIOUS_CSV_SIZE)) ; then
   cp $PBP_OUT_DIR/transfers.csv $PBP_OUT_DIR/transfers.csv.SAVED
else
   echo "build_transfer_filter: Last good CSV [$PREVIOUS_CSV_SIZE] was bigger than today's [$TODAY_CSV_SIZE] so using that"
   cp $PBP_OUT_DIR/transfers.csv.SAVED $PBP_OUT_DIR/transfers.csv
fi

echo "" > $PBP_OUT_DIR/nba_declarations_$CURR_YEAR_P1.html
if [ "$CURR_YEAR_P1" -eq "2022" ]; then
   curl -o $PBP_OUT_DIR/nba_declarations_$CURR_YEAR_P1.html "https://www.hoopsrumors.com/2022/03/2022-nba-draft-early-entrants-list.html"
fi
if [ "$CURR_YEAR_P1" -eq "2021" ]; then
   curl -o $PBP_OUT_DIR/nba_declarations_$CURR_YEAR_P1.html "https://www.hoopsrumors.com/2021/04/2021-nba-draft-early-entrants-list.html"
fi
if [ "$CURR_YEAR_P1" -eq "2020" ]; then
   curl -o $PBP_OUT_DIR/nba_declarations_$CURR_YEAR_P1.html "https://www.hoopsrumors.com/2020/03/2020-nba-draft-early-entrants-list.html"
fi

NUM_TRANSFERS=$(wc -l $PBP_OUT_DIR/transfers.csv | awk '{ print $1 }')
NUM_NBA=$(grep -c 'www.sports-reference.com' $PBP_OUT_DIR/nba_declarations_$CURR_YEAR_P1.html) 
echo "build_transfer_filter: [$(date)] Downloaded [$NUM_NBA] lines of NBA declarations"
echo "build_transfer_filter: [$(date)] Downloaded [$NUM_TRANSFERS] transfers"

if [[ $NUM_TRANSFERS -gt 0 ]] && [[ $NUM_NBA -gt 0 ]]; then
   java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
      org.piggottfamily.cbb_explorer.BuildTransferLookup --in=$PBP_OUT_DIR/transfers.csv \
      --in-nba=$PBP_OUT_DIR/nba_declarations_$CURR_YEAR_P1.html \
      --rosters=$HOOPEXP_SRC_DIR/public/rosters --out=$PBP_OUT_DIR/transfers_$CURR_YEAR_P1.json --year=$CURR_YEAR

   gsutil cp $PBP_OUT_DIR/transfers_$CURR_YEAR_P1.json gs://$LEADERBOARD_BUCKET/ 
fi
