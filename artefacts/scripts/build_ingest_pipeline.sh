#!/bin/bash

# mid majors:
#CONFS="wcc,mountainwest,mvc,conferenceusa,mac,socon,sunbelt,bigsky,colonial,summit"

java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
  org.piggottfamily.cbb_explorer.BuildIngestPipeline \
  --out=$PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/ \
  --in=$IN_HTML \
  --gender=$GENDER \
  --confs=$CONFS \
  --replace-existing=no
