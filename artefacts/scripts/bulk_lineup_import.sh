#!/bin/bash

export CURR_TIME=${CURR_TIME:=$(date +"%s")}
echo ">>>>>>> Extracting from [$CURR_TIME]"
sleep 2

export CURR_YEAR=${CURR_YEAR:="2019"}
export CURR_YEAR_STR=${CURR_YEAR_STR:="2019_20"}

export CONFS="acc american atlanticten bigeast bigten bigtwelve pactwelve sec misc_conf"
#export CONFS="misc_conf"
#export CONFS="bigten"

for i in $CONFS; do
  echo "******* Extracting conference [$i]"
  $PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/${i}/${CURR_YEAR_STR}/lineups-cli.sh

  java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
    org.piggottfamily.cbb_explorer.BuildLineups \
    --in=$PBP_CRAWL_PATH/${i}/${CURR_YEAR}/ \
    --out=$PBP_OUT_DIR \
    --from=$CURR_TIME
done
