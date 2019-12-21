#!/bin/bash

export CURR_TIME=${CURR_TIME:=$(date +"%s")}

export CURR_YEAR=${CURR_YEAR:="2019"}
export CURR_YEAR_STR=${CURR_YEAR_STR:="2019_20"}

export CONFS=${CONFS:="acc american atlanticten bigeast bigten bigtwelve pactwelve sec misc_conf"}
#export CONFS="women_bigten"

echo ">>>>>>> Extracting from [$CURR_TIME] for [$CURR_YEAR]/[$CURR_YEAR_STR] on [$CONFS]"
sleep 2

mkdir -p $PBP_OUT_DIR/archive
mv $PBP_OUT_DIR/*.ndjson $PBP_OUT_DIR/archive

rm -f $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log
for i in $CONFS; do
  echo "******* Extracting conference [$i]"
  $PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/${i}/${CURR_YEAR_STR}/lineups-cli.sh

  java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
    org.piggottfamily.cbb_explorer.BuildLineups \
    --in=$PBP_CRAWL_PATH/${i}/${CURR_YEAR}/ \
    --out=$PBP_OUT_DIR \
    --from=$CURR_TIME >> $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log
done

# Output a summary of the bulk import:
grep "LineupErrorAnalysis" $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log

# Import:
echo "Importing new game data..."
$ELASTIC_FILEBEAT_BIN -c $ELASTIC_FILEBEAT_CONFIG_ROOT/filebeat_lineups.yaml --once
