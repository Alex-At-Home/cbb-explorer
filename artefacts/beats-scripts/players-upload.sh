
export CURR_YEAR_STR=${CURR_YEAR_STR:="2025_26"}
export CURR_YEAR=$(echo $CURR_YEAR_STR | cut -c1-4)

#for i in "2018/9" "2019/20" "2020/21" "2021/22" "2022/23" "2023/24" "2024/25" "2025_26"; do echo $i; CURR_YEAR_STR=$i sh ../../cbb-explorer/artefacts/beats-scripts/players-upload.sh; sleep 2;  done
#for i in "2021/22" "2022/23" "2023/24" "2024/25" "2025_26"; do echo $i; CURR_YEAR_STR=$i sh ../../cbb-explorer/artefacts/beats-scripts/players-upload.sh; sleep 2;  done
# Use GENDER_FILTER="men" || GENDER_FILTER="women" if you want only one gender

export CLOSE_EOF="true"
export CURR_TIME=${CURR_TIME:=$(date +"%s")}

export PING="lping" #(no need for ping/pong right now)

# Archive any existing files

echo "(archiving existing files)"

mv ${PBP_OUT_DIR}/men_playerproc_* ${PBP_OUT_DIR}/archive 
mv ${PBP_OUT_DIR}/women_playerproc_* ${PBP_OUT_DIR}/archive 

# Convert the JSON files into ndjson format

echo "(formatting new files from ${CURR_YEAR})"

if [ "$GENDER_FILTER" != "women" ]; then
   for i in all conf t100 lowvol; do 
      cat ${HOOPEXP_SRC_DIR}/enrichedPlayers/players_${i}_Men_${CURR_YEAR}_*.json  | jq -c '.players[] | .year |= .[0:4]' > ${PBP_OUT_DIR}/men_playerproc_${CURR_TIME}_${i}.ndjson
   done
fi
if [ "$GENDER_FILTER" != "men" ]; then
   for i in all conf t100 lowvol; do 
      cat ${HOOPEXP_SRC_DIR}/enrichedPlayers/players_${i}_Women_${CURR_YEAR}_*.json  | jq -c '.players[] | .year |= .[0:4]' > ${PBP_OUT_DIR}/women_playerproc_${CURR_TIME}_${i}.ndjson
   done
fi

if [ "$GENDER_FILTER" != "women" ]; then
   echo "formatted $(wc -l ${PBP_OUT_DIR}/men_playerproc_${CURR_TIME}*.ndjson | awk '{ print $1 }') men"
fi
if [ "$GENDER_FILTER" != "men" ]; then
   echo "formatted $(wc -l ${PBP_OUT_DIR}/women_playerproc_${CURR_TIME}*.ndjson | awk '{ print $1 }') women"
fi

# Invoke filebeat to ingest them

echo "(uploading men and women)"

if [ "$DRY_RUN" != "yes" ]; then
   $ELASTIC_FILEBEAT_BIN -E PING="$PING" -E CLOSE_EOF="$CLOSE_EOF" -c $ELASTIC_FILEBEAT_CONFIG_ROOT/filebeat_players.yaml --once
else
   echo "(Dry run)"
fi
echo "(done)"
