#!/bin/bash

echo "Fix broken play-by-play files"

for i in $(find $PBP_CRAWL_PATH -name "*.zip" | grep "/$CURR_YEAR/"); do
   j=$(unzip -l $i | grep box_score | grep -E "\s+[0-9][0-9][0-9]?\s+" | grep -o 'https:.*') && echo "$i /// $j" && zip -d $i "$j";
done

############

echo "Download / parse / upload new data"

cd $PBP_SRC_ROOT
source .scripts.env
cd $PBP_OUT_DIR
PING="lpong" DOWNLOAD="yes" PARSE="yes" UPLOAD="yes" sh $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
cd $HOOPEXP_SRC_DIR
source .env
sh handle-updated-data.sh
