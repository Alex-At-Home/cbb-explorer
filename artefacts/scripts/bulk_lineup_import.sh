#!/bin/bash
# If running interactively set CLOSE_EOF=true

# Set this to be $(date -v -1d '+%m:%d:%Y') in order to run the script in minimal collection mode
# GAME_BASED_FILTER=$(date -v -1d '+%m:%d:%Y')

# To get just the roster and player pages (to get the lowest NCAA id, since there is no UUID)
# set ROSTER_DOWNLOAD_ONLY=yes

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
# Use + instead of " " in this filter:
if [ ! -z "$TEAM_FILTER" ]; then
  export TEAM_FILTER="--team=$TEAM_FILTER"
fi

export CLOSE_EOF=${CLOSE_EOF:="false"}

export CURR_TIME=${CURR_TIME:=$(date +"%s")}

export CURR_YEAR_STR=${CURR_YEAR_STR:="2024_25"}
export CURR_YEAR=$(echo $CURR_YEAR_STR | cut -c1-4)
export ACADEMIC_YEAR=$(echo "20$(echo $CURR_YEAR_STR | cut -c6-7)")

# TOOD: removed pactwelve / women_pactwelve for now
#export CONFS=${CONFS:="acc american atlanticten bigeast bigten bigtwelve sec wcc mountainwest mvc conferenceusa mac socon sunbelt bigsky colonial summit americaeast atlanticsun bigsouth bigwest horizon ivy maac meac nec ovc patriot southland swac wac women_acc women_american women_bigeast women_bigten women_bigtwelve women_sec women_misc_conf"}
export CONFS=${CONFS:="pactwelve women_pactwelve acc american atlanticten bigeast bigten bigtwelve sec wcc mountainwest mvc conferenceusa mac socon sunbelt bigsky colonial summit americaeast atlanticsun bigsouth bigwest horizon ivy maac meac nec ovc patriot southland swac wac women_acc women_american women_bigeast women_bigten women_bigtwelve women_sec women_misc_conf"}
#export CONFS=${CONFS:="acc american atlanticten bigeast bigten bigtwelve pactwelve sec misc_conf"}
#export CONFS=${CONFS:="women_acc women_american women_bigeast women_bigten women_bigtwelve women_pactwelve women_sec women_misc_conf"}
#export CONFS=${CONFS:="wcc mountainwest mvc conferenceusa mac socon sunbelt bigsky colonial summit"}
#export CONFS=${CONFS:="americaeast atlanticsun bigsouth bigwest horizon ivy maac meac nec ovc patriot southland swac wac"}

echo ">>>>>>> Extracting from [$CURR_TIME] for [$CURR_YEAR]/[$CURR_YEAR_STR] on [$CONFS] with [$TEAM_FILTER/$TEAMID_FILTER]"
sleep 2

if [ "$PARSE" == "yes" ]; then
  echo "Clearing out previously parsed PBP files"
  mkdir -p $PBP_OUT_DIR/archive
  mv $PBP_OUT_DIR/*.ndjson $PBP_OUT_DIR/archive
fi

rm -f $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log

if [ "$DOWNLOAD" == "yes" ]; then
  # New logic to collect only game that have occurred
  if [ "$GAME_BASED_FILTER" != "" ]; then
    echo "Collecting games from [$GAME_BASED_FILTER]"

    GAME_BASED_FILTER_SUFFIX=$(echo "$GAME_BASED_FILTER" | sed 's/:/_/g')
    # Note this is only applicable for a given season, for previous seasons need to use the old download way
    # at start of season need to change the season_divisions id
    if true; then
      curl -o "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_men.html" \
            -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Safari/605.1.15' \
        "https://stats.ncaa.org/contests/livestream_scoreboards?utf8=%E2%9C%93&sport_code=MBB&academic_year=${ACADEMIC_YEAR}&division=1&game_date=$(echo $GAME_BASED_FILTER | sed 's/:/\%2F/g')&commit=Submit"
      curl -o "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_women.html" \
            -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Safari/605.1.15' \
        "https://stats.ncaa.org/contests/livestream_scoreboards?utf8=%E2%9C%93&sport_code=WBB&academic_year=${ACADEMIC_YEAR}&division=1&game_date=$(echo $GAME_BASED_FILTER | sed 's/:/\%2F/g')&commit=Submit"
    fi
    cat "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_men.html" | grep -E -o 'src="https:.*/[0-9]+.gif"' | sed -E 's|.*/([0-9]+)[.]gif"|_\1.0_|g' > "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_men.txt"
    cat "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_women.html" | grep -E -o 'src="https:.*/[0-9]+.gif"' | sed -E 's|.*/([0-9]+)[.]gif"|_\1.0_|g' > "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_women.txt"

    echo "Found [$(wc -l "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_men.txt" | awk '{ print $1 }')] for men and [$(wc -l "ncaa_games_${GAME_BASED_FILTER_SUFFIX}_women.txt" | awk '{ print $1 }')] for women"
  fi
fi

for i in $CONFS; do
  echo "******* Extracting conference [$i]"
  if [ "$DOWNLOAD" == "yes" ]; then
    echo "Downloading PBP files..."

    if [ "$GAME_BASED_FILTER" != "" ]; then
      if echo $i | grep -q "women_"; then
        GAME_BASED_FILTER_FILE="$(pwd)/ncaa_games_${GAME_BASED_FILTER_SUFFIX}_women.txt"
      else
        GAME_BASED_FILTER_FILE="$(pwd)/ncaa_games_${GAME_BASED_FILTER_SUFFIX}_men.txt"
      fi
      echo "Using [$GAME_BASED_FILTER_FILE] to filter games"
    else
      unset $GAME_BASED_FILTER
    fi

    GAME_BASED_FILTER_FILE=$GAME_BASED_FILTER_FILE \
      $PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/${i}/${CURR_YEAR_STR}/lineups-cli.sh \
      | tee $PBP_OUT_DIR/tmp_download_logs.txt

    # Some error checking for a common httrack/server issue that crops up:
    # (see also daily_cbb_import.sh - but this version fixes it on the fly)
    if grep -q -F "************ ERRORS" $PBP_OUT_DIR/tmp_download_logs.txt; then
      echo "Found errors in PBP downloads, see [$PBP_OUT_DIR/tmp_download_errs_$i.txt]"
      grep -F "************ ERRORS" $PBP_OUT_DIR/tmp_download_logs.txt |  cut -d" " -f3 > $PBP_OUT_DIR/tmp_download_errs_$i.txt
      for err_file in $(cat $PBP_OUT_DIR/tmp_download_errs_$i.txt); do
        echo "Checking [$err_file] for possible fix"
        grep "Error" $err_file | grep "50[03]" | grep -o "at link https://[^ ]*" | grep -o "https://[^ ]*" > /tmp/cbb-explorer-tofix
        if [ -s /tmp/cbb-explorer-tofix ]; then
          export ERR_DIR=$(dirname $err_file)
          export ERR_LINK=$(cat /tmp/cbb-explorer-tofix | head -n 1)
          echo "Found fixable issue: [zip -d $ERR_DIR/hts-cache/new.zip $ERR_LINK]"
          zip -d $ERR_DIR/hts-cache/new.zip $ERR_LINK
          echo "Now retry download (ignoring any further errors until next run) after a 10m wait"
          sleep 600
          $PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/${i}/${CURR_YEAR_STR}/lineups-cli.sh \
            | tee $PBP_OUT_DIR/tmp_download_logs_2.txt

          if ! grep -q -F "************ ERRORS" $PBP_OUT_DIR/tmp_download_logs_2.txt; then
            echo "************ ERRORS: FIXED [$PBP_OUT_DIR/tmp_download_errs_$i.txt]"
          fi
          rm -f $PBP_OUT_DIR/tmp_download_logs_2.txt
        fi
      done
    fi
    rm -f $PBP_OUT_DIR/tmp_download_logs.txt
  else
    echo "Skipping download"
  fi

  if [ "$PARSE" == "yes" ]; then
    echo "Parsing PBP files..."
    java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
      org.piggottfamily.cbb_explorer.BuildLineups \
      --in=$PBP_CRAWL_PATH/${i}/${CURR_YEAR}/ \
      --out=$PBP_OUT_DIR \
      --player-events --shot-events $TEAM_FILTER \
      --from=$CURR_TIME >> $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log
  else
    echo "Skipping parse"
  fi
done

# Output a summary of the bulk import:
if [ "$SHOW_ALL_LOGS" == "yes" ]; then
  cat $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log
else
  grep "LineupErrorAnalysis" $PBP_OUT_DIR/bulk_import_logs_${CURR_TIME}.log
fi

# Import:
if [ "$UPLOAD" == "yes" ]; then
  echo "Uploading new game data..."
  if [ "$(pwd)" != "$PBP_OUT_DIR" ]; then
    echo "Warning: need to be in [$PBP_OUT_DIR], currently in $(pwd)"
  fi 
  $ELASTIC_FILEBEAT_BIN -E PING="$PING" -E CLOSE_EOF="$CLOSE_EOF" -c $ELASTIC_FILEBEAT_CONFIG_ROOT/filebeat_lineups.yaml --once
else
  echo "Skipping upload"
fi
