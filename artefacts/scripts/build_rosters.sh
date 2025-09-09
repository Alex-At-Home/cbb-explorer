#!/bin/bash

if [ ! -z "$TEAM_FILTER" ]; then
  export TEAM_FILTER="--team=$TEAM_FILTER"
fi

export CURR_YEAR_STR=${CURR_YEAR_STR:="2024_25"}
export CURR_YEAR=$(echo $CURR_YEAR_STR | cut -c1-4)

export CONFS=${CONFS:="acc american atlanticten bigeast bigten bigtwelve pactwelve sec wcc mountainwest mvc conferenceusa mac socon sunbelt bigsky colonial summit americaeast atlanticsun bigsouth bigwest horizon ivy maac meac nec ovc patriot southland swac wac women_acc women_american women_bigeast women_bigten women_bigtwelve women_pactwelve women_sec women_misc_conf"}
#export CONFS=${CONFS:="acc american atlanticten bigeast bigten bigtwelve pactwelve sec misc_conf"}
#export CONFS=${CONFS:="women_acc women_american women_bigeast women_bigten women_bigtwelve women_pactwelve women_sec women_misc_conf"}
#export CONFS=${CONFS:="wcc mountainwest mvc conferenceusa mac socon sunbelt bigsky colonial summit"}
#export CONFS=${CONFS:="americaeast atlanticsun bigsouth bigwest horizon ivy maac meac nec ovc patriot southland swac wac"}

echo ">>>>>>> Extracting for [$CURR_YEAR]/[$CURR_YEAR_STR] on [$CONFS] with [$TEAM_FILTER]"

for c in $CONFS; do
  echo "******* Extracting conference [$c]"

  echo "Cleansing invalid roster files... (may need to re-download if you see an error)"
  for i in $(find $PBP_CRAWL_PATH/${c}/${CURR_YEAR}/ -name "*.zip" | grep "/$CURR_YEAR/"); do
     j=$(unzip -l $i | grep '/team/' | grep '/roster/' | grep -E "\s+[0-9][0-9]?[0-9]?\s+" | grep -o 'https:.*') && echo "*************** $i /// $j" && zip -d $i "$j";
  done

  echo "Parsing roster files..."
  java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
    org.piggottfamily.cbb_explorer.BuildRosters \
    --in=$PBP_CRAWL_PATH/${c}/${CURR_YEAR} \
    --out=$HOOPEXP_SRC_DIR/public/rosters/ \
    $TEAM_FILTER
done
