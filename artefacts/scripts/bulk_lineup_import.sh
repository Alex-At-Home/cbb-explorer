#!/bin/bash

if [ "$PING" != "lping" ] && [ "$PING" != "lpong" ] && [ "$PING" != "ltest" ]; then
  echo "Need to specify PING env var as either 'lping' or 'lpong' (or 'ltest'), not '$PING'"
  exit -1
fi

if [ "$DOWNLOAD" != "yes" ] && [ "$DOWNLOAD" != "no" ]; then
  echo "Need to specify DOWNLOAD/PARSE/UPLOAD as yes or no [DOWNLOAD]"
fi
if [ "$PARSE" != "yes" ] && [ "$PARSE" != "no" ]; then
  echo "Need to specify DOWNLOAD/PARSE/UPLOAD as yes or no [PARSE]"
fi
if [ "$UPLOAD" != "yes" ] && [ "$UPLOAD" != "no" ]; then
  echo "Need to specify DOWNLOAD/PARSE/UPLOAD as yes or no [UPLOAD]"
fi
if [ ! -z "$TEAM_FILTER" ]; then
  export TEAM_FILTER="--team=$TEAM_FILTER"
fi

export CURR_TIME=${CURR_TIME:=$(date +"%s")}

export CURR_YEAR_STR=${CURR_YEAR_STR:="2021_22"}
export CURR_YEAR=$(echo $CURR_YEAR_STR | cut -c1-4)

export CONFS=${CONFS:="acc american atlanticten bigeast bigten bigtwelve pactwelve sec wcc mountainwest mvc conferenceusa mac socon sunbelt bigsky colonial summit americaeast atlanticsun bigsouth bigwest horizon ivy maac meac nec ovc patriot southland swac wac women_acc women_american women_bigeast women_bigten women_bigtwelve women_pactwelve women_sec women_misc_conf"}
#export CONFS=${CONFS:="acc american atlanticten bigeast bigten bigtwelve pactwelve sec misc_conf"}
#export CONFS=${CONFS:="women_acc women_american women_bigeast women_bigten women_bigtwelve women_pactwelve women_sec women_misc_conf"}
#export CONFS=${CONFS:="wcc mountainwest mvc conferenceusa mac socon sunbelt bigsky colonial summit"}
#export CONFS=${CONFS:="americaeast atlanticsun bigsouth bigwest horizon ivy maac meac nec ovc patriot southland swac wac"}

echo ">>>>>>> Extracting from [$CURR_TIME] for [$CURR_YEAR]/[$CURR_YEAR_STR] on [$CONFS] with [$TEAM_FILTER]"
sleep 2

if [ "$PARSE" == "yes" ]; then
  echo "Clearing out previously parsed PBP files"
  mkdir -p $PBP_OUT_DIR/archive
  mv $PBP_OUT_DIR/*.ndjson $PBP_OUT_DIR/archive
fi

rm -f $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log
for i in $CONFS; do
  echo "******* Extracting conference [$i]"
  if [ "$DOWNLOAD" == "yes" ]; then
    echo "Downloading PBP files..."
    $PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/${i}/${CURR_YEAR_STR}/lineups-cli.sh
  else
    echo "Skipping download"
  fi

  if [ "$PARSE" == "yes" ]; then
    echo "Parsing PBP files..."
    java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
      org.piggottfamily.cbb_explorer.BuildLineups \
      --in=$PBP_CRAWL_PATH/${i}/${CURR_YEAR}/ \
      --out=$PBP_OUT_DIR \
      --player-events $TEAM_FILTER \
      --from=$CURR_TIME >> $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log
  else
    echo "Skipping parse"
  fi
done

# Output a summary of the bulk import:
grep "LineupErrorAnalysis" $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log

# Import:
if [ "$UPLOAD" == "yes" ]; then
  echo "Uploading new game data..."
  $ELASTIC_FILEBEAT_BIN -E PING="$PING" -c $ELASTIC_FILEBEAT_CONFIG_ROOT/filebeat_lineups.yaml --once
else
  echo "Skipping upload"
fi
