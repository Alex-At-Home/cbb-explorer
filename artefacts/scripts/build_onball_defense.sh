#!/bin/sh
java -cp \
   "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
   org.piggottfamily.cbb_explorer.BuildOnBallDefense \
   --in-team=$PBP_OUT_DIR/OnBallDefense/Teams_23_24.csv \
   --in-player=$PBP_OUT_DIR/OnBallDefense/Indiv_23_24.csv \
   --rosters=$HOOPEXP_SRC_DIR/public/rosters \
   --out=$PBP_OUT_DIR/OnBallDefense/out \
   --year=2023
