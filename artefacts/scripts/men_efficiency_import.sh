#!/bin/bash

export CURR_TIME=${CURR_TIME:=$(date +"%s")}

export CURR_YEAR=${CURR_YEAR:="2020"}

#export CONFS="women_bigten"

echo ">>>>>>> Extracting from [$CURR_TIME] for [$CURR_YEAR]"

mkdir -p $PBP_OUT_DIR/archive
mv $PBP_OUT_DIR/*.ndjson $PBP_OUT_DIR/archive

rm -f $PBP_OUT_DIR/efficiency_logs_${CURR_TIME}.log

#TODO: bring in cookie and perform
#TODO handle failures
#$PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/${i}/${CURR_YEAR_STR}/lineups-cli.sh

java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
    org.piggottfamily.cbb_explorer.BuildEfficiency \
    --in=$EFF_CRAWL_PATH/kenpom.com/ \
    --out=$PBP_OUT_DIR \
    --year=$CURR_YEAR >> $PBP_OUT_DIR/efficiency_logs_${CURR_TIME}.log
#TODO handle failures

#TODO delete existing data
#TODO upload data

# Show any errors:
grep "ERROR" $PBP_OUT_DIR/efficiency_logs_${CURR_TIME}.log
