#!/bin/bash
# Update this every year then call with no args

export REBUILD_EXISTING=${REBUILD_EXISTING:="false"}
export PREV_YEAR=${PREV_YEAR:=2024_25}
export PREV_YEAR_START=${PREV_YEAR:0:4}
export NEXT_YEAR=${NEXT_YEAR:=2025_26}
export NEXT_YEAR_START=${NEXT_YEAR:0:4}
export MEN_PREV_ID=16700
export MEN_NEXT_ID=16960
export WOMEN_PREV_ID=16720
export WOMEN_NEXT_ID=16961

echo "You should first duplicate and build the men's efficiency gsheet for this season and check for any new teams!"

echo "Summary:"
echo "REBUILD_EXISTING=$REBUILD_EXISTING PREV_YEAR=$PREV_YEAR PREV_YEAR_START=$PREV_YEAR_START NEXT_YEAR=$NEXT_YEAR NEXT_YEAR_START=$NEXT_YEAR_START"
echo "MEN_PREV_ID=$MEN_PREV_ID MEN_NEXT_ID=$MEN_NEXT_ID WOMEN_PREV_ID=$WOMEN_PREV_ID WOMEN_NEXT_ID=$WOMEN_NEXT_ID"

for conf_dir in $(find $PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/ -depth 1 -a -type d); do
   echo "Processing conf [$conf_dir]"
   if [ -d $conf_dir/$PREV_YEAR ]; then
      if [ -d $conf_dir/$NEXT_YEAR ]; then
         if [ "$REBUILD_EXISTING" == "true" ]; then
            echo "Deleting existing [$conf_dir/$NEXT_YEAR]"
             rm -rf $conf_dir/$NEXT_YEAR
         else 
            echo "Skipping existing [$conf_dir/$NEXT_YEAR]"
            continue
         fi
      fi
      mkdir -p $conf_dir/$NEXT_YEAR/
      cp -f $conf_dir/$PREV_YEAR/* $conf_dir/$NEXT_YEAR/
      if [ ! -f $conf_dir/$NEXT_YEAR/lineups-cli.sh ]; then
         echo "ERROR: no [$conf_dir/$NEXT_YEAR//lineups-cli.sh]"
         exit 1
      fi
      # Men
      sed -i "" "s|/$MEN_PREV_ID::|/$MEN_NEXT_ID::|" $conf_dir/$NEXT_YEAR/lineups-cli.sh
      if ! grep -q "/$MEN_NEXT_ID::" $conf_dir/$NEXT_YEAR/lineups-cli.sh; then
         # Maybe it's a women's team:

         # Women
         sed -i "" "s|/$WOMEN_PREV_ID::|/$WOMEN_NEXT_ID::|" $conf_dir/$NEXT_YEAR/lineups-cli.sh
         if ! grep -q "/$WOMEN_NEXT_ID::" $conf_dir/$NEXT_YEAR/lineups-cli.sh; then
            echo "Failed to change [$conf_dir/$NEXT_YEAR/lineups-cli.sh] from [$WOMEN_PREV_ID] to [$WOMEN_NEXT_ID]"
            echo "AND/OR"
            echo "Failed to change [$conf_dir/$NEXT_YEAR/lineups-cli.sh] from [$MEN_PREV_ID] to [$MEN_NEXT_ID]"
            exit 1
         fi
      fi
      # Year
      sed -i "" "s|YEAR=$PREV_YEAR_START|YEAR=$NEXT_YEAR_START|" $conf_dir/$NEXT_YEAR/lineups-cli.sh
      if ! grep -q "YEAR=$NEXT_YEAR_START" $conf_dir/$NEXT_YEAR/lineups-cli.sh; then
         echo "Failed to change [$conf_dir/$NEXT_YEAR/lineups-cli.sh] from [YEAR=$PREV_YEAR_START] to [YEAR=$NEXT_YEAR_START]"
         exit 1
      fi
   else
      echo "WARNING: no [$conf_dir/$PREV_YEAR/]"
   fi
done
echo "Now you need to go cut/paste conference changes!"
