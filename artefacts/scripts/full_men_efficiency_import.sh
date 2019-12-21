#!/bin/bash

export CURR_TIME=${CURR_TIME:=$(date +"%s")}
export CURR_YEAR=${CURR_YEAR:="2020"}

if [ -z "$COOKIE" ]; then
  echo "It is necessary to include the auth cookie as env var COOKIE"
  exit -1
fi

echo ">>>>>>> Extracting from [$CURR_TIME] for [$CURR_YEAR]"

mkdir -p $PBP_OUT_DIR/archive
mv $PBP_OUT_DIR/*.ndjson $PBP_OUT_DIR/archive

rm -f $PBP_OUT_DIR/efficiency_logs_${CURR_TIME}.log

export COOKIE_FRAGMENT=$(echo "$COOKIE" | grep -E -o "[a-z0-9]{26}" )
echo "$EFF_ROOT_URL TRUE / FALSE 1999999999 PHPSESSID $COOKIE_FRAGMENT" | tr " " \\t > $PWD/cookies.txt
echo "Using [$COOKIE_FRAGMENT] for authentication"

$PBP_SRC_ROOT/artefacts/httrack-scripts/efficiency-cli-curryear.sh

# Can't easily detect failures so just always retry
echo "Retrying x1 to catch any stragglers (after 1min pause)"
sleep 60
$PBP_SRC_ROOT/artefacts/httrack-scripts/efficiency-cli-curryear.sh --retry

java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
    org.piggottfamily.cbb_explorer.BuildEfficiency \
    --in=$EFF_CRAWL_PATH/${EFF_ROOT_URL}/ \
    --out=$PBP_OUT_DIR \
    --year=$CURR_YEAR >> $PBP_OUT_DIR/efficiency_logs_${CURR_TIME}.log

# Show any errors:
grep "ERROR" $PBP_OUT_DIR/efficiency_logs_${CURR_TIME}.log

# Always re-import regardless, it just overwrites any existing records
# (once imported for the first time using the "kenpom_id_pipeline")
echo "Re-importing/updating new records":
echo "$ELASTIC_FILEBEAT_BIN -c $ELASTIC_FILEBEAT_CONFIG_ROOT/filebeat_efficiency.yaml --once"
$ELASTIC_FILEBEAT_BIN -c $ELASTIC_FILEBEAT_CONFIG_ROOT/filebeat_efficiency.yaml --once
