#!/bin/bash
# Call with --skip-download to bypass the download and just process/upload
# Run from ELASTIC_FILEBEAT_CONFIG_ROOT (eg "cbb-data/cbb")
# Ensure ".scripts.env" has been run

export CURR_TIME=${CURR_TIME:=$(date +"%s")}
export CURR_YEAR=${CURR_YEAR:="2022"}

if [ -z "$COOKIE" ]; then
  echo "No cookie, trying to login manually"
  COOKIE=$(curl --silent --cookie-jar - -XPOST 'https://kenpom.com/handlers/login_handler.php' -d"email=${EFF_USER}&password=${EFF_PASSWORD}&submit=Login" | grep PHPSESSID | awk '{ print $7 }')

  if [ -z "$COOKIE" ]; then
    echo "It is necessary to include the auth cookie as env var COOKIE, or the user/pass as EFF_USER/EFF_PASSWORD"
    exit -1
  else
    echo "Login successful"
  fi
fi

echo ">>>>>>> Extracting from [$CURR_TIME] for [$CURR_YEAR]"

mkdir -p $PBP_OUT_DIR/archive
mv $PBP_OUT_DIR/*.ndjson $PBP_OUT_DIR/archive

rm -f $PBP_OUT_DIR/efficiency_logs_${CURR_TIME}.log

if [ "$1" != "--skip-download" ]; then
  export COOKIE_FRAGMENT=$(echo "$COOKIE" | grep -E -o "[a-z0-9]{26}" )
  sed s/COOKIE_FRAGMENT/"$COOKIE_FRAGMENT"/ $PWD/cookies_template.txt > $PWD/cookies.txt
  echo "Using [$COOKIE_FRAGMENT] for authentication"

  $PBP_SRC_ROOT/artefacts/httrack-scripts/efficiency-cli-curryear.sh || exit -1

  # Can't easily detect failures so just always retry
  echo "Retrying x1 to catch any stragglers (after 1min pause)"
  sleep 60
  $PBP_SRC_ROOT/artefacts/httrack-scripts/efficiency-cli-curryear.sh --retry || exit -1
fi

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
