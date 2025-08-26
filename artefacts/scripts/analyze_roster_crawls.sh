#!/usr/bin/env bash
set -euo pipefail

# Run this like
# CURR_YEAR=2024 DRY_RUN=yes|no ./artefacts/scripts/analyze_roster_crawls.sh 
# debug:
# OVERRIDE_DIR=$path/NCAA_by_conf/women_acc/2024/North+Carolina_457.0 DRY_RUN=yes|no ./artefacts/scripts/analyze_roster_crawls.sh 
# to delete JSONs representing failed files
# (then re-running the script will re-download them)

echo "Searching for failed persistent crawl JSON files under $PBP_CRAWL_PATH (year [$CURR_YEAR]) (DRY_RUN=[$DRY_RUN]) ..."

OVERRIDE_DIR=${OVERRIDE_DIR:-""}
if [ "$OVERRIDE_DIR" != "" ]; then
   echo "Looking in [$OVERRIDE_DIR]"
   find "$OVERRIDE_DIR/roster_crawl" -type f -path "*/roster_crawl/request_queues/persistent_crawl_state/*.json" | while read -r file; do
      echo "Looking in [$file]"

      # Skip files that have retryCount = 0 (fast grep)
      if grep -q '"retryCount": 0' "$file"; then
        continue
      fi

      # extract inner "json" string and parse it
      inner_json=$(jq -r '.json' "$file")
      # check if errorMessages array has length > 0
      if [ -n "$inner_json" ] && echo "$inner_json" | jq -e '.errorMessages | length > 0' &>/dev/null; then
         echo "❌ Error JSON: $file"
         # Need to remove the top-level teams + roster so we re-add the players
         for other_file in "$OVERRIDE_DIR/roster_crawl/request_queues/persistent_crawl_state"/*.json; do
            if grep -q 'uniqueKey.*/team' $other_file; then
               echo "(Means need to delete [$other_file] to force recrawl)"
               if [ "$DRY_RUN" == "no" ]; then
                  echo "(not in dry run: removed [$other_file])"
                  rm -f $other_file
               fi
            fi
         done
         if [ "$DRY_RUN" == "no" ]; then
            echo "(not in dry run: removed [$file])"
            rm -f $file
         fi
      fi
   done
   exit 0
fi

# loop through each conference
for conf_dir in "$PBP_CRAWL_PATH"/*; do
  if [[ -d "$conf_dir/$CURR_YEAR" ]]; then
    # search only inside $conf_dir/$YEAR
    echo "Looking in [$conf_dir/$CURR_YEAR]"
    for team_dir in "$conf_dir/$CURR_YEAR"/*; do
       #echo "Looking in [$team_dir]"
      if [[ -d "$team_dir/roster_crawl" ]]; then
         find "$team_dir/roster_crawl" -type f -path "*/roster_crawl/request_queues/persistent_crawl_state/*.json" | while read -r file; do
            if [ ! -f $file ]; then
               echo "(skip deleted file [$file]), 1"
               continue
            fi
            #echo "Looking in [$file]"
            # Skip files that have retryCount = 0 (fast grep)
            if grep -q '"retryCount": 0' "$file"; then
               continue
            fi

            # extract inner "json" string and parse it
            inner_json=$(jq -r '.json' "$file")
            # check if errorMessages array has length > 0
            if [ -n "$inner_json" ] && echo "$inner_json" | jq -e '.errorMessages | length > 0' &>/dev/null; then
               echo "❌ Error JSON: $file"
               # Need to remove the top-level teams + roster so we re-add the players
               for other_file in "$team_dir/roster_crawl/request_queues/persistent_crawl_state"/*.json; do
                  if [ ! -f $other_file ]; then
                     echo "(skip deleted file [$other_file], 2)"
                     continue
                  fi

                  if grep -q 'uniqueKey.*/team' $other_file; then
                     echo "(Means need to delete [$other_file] to force recrawl)"
                     if [ "$DRY_RUN" == "no" ]; then
                        echo "(not in dry run: removed [$other_file])"
                        rm -f $other_file
                     fi
                  fi
               done
               if [ "$DRY_RUN" == "no" ]; then
                  echo "(not in dry run: removed [$file])"
                  rm -f $file
               fi
            fi
         done
      fi
    done
  fi
done
