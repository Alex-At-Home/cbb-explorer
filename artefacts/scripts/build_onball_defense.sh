#!/bin/sh
SEASON=25_26
YEAR=2025
if [ "$GENDER" == "Women" ]; then
   ONBALL_DIRNAME=WomenOnBallDefense
else
   ONBALL_DIRNAME=OnBallDefense
   GENDER=Men
fi
java -cp \
   "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
   org.piggottfamily.cbb_explorer.BuildOnBallDefense \
   --in-team=$PBP_OUT_DIR/$ONBALL_DIRNAME/Teams_${SEASON}.csv \
   --in-player=$PBP_OUT_DIR/$ONBALL_DIRNAME/Indiv_${SEASON}.csv \
   --rosters=$HOOPEXP_SRC_DIR/public/rosters/${GENDER}_$YEAR \
   --out=$PBP_OUT_DIR/$ONBALL_DIRNAME/out \
   --year=$YEAR

# Getting the data examples:
# cp ~/Downloads/Leaderboards\ -\ College\ Women\ 2025-2026\ All\ excluding\ Exhibitions\ -\ Overall\ -\ Player\ Defensive.csv WomenOnBallDefense/Indiv_25_26.csv
# cp ~/Downloads/Leaderboards\ -\ College\ Women\ 2025-2026\ All\ excluding\ Exhibitions\ -\ Overall\ -\ Team\ Defensive.csv WomenOnBallDefense/Teams_25_26.csv