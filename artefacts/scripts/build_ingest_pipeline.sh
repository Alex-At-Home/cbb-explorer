#!/bin/bash

# mid majors:
#CONFS="wcc,mountainwest,mvc,conferenceusa,mac,socon,sunbelt,bigsky,colonial,summit"

#--in comes from "https://stats.ncaa.org/rankings/change_sport_year_div" (basketball > year > DI > Team > Scoring Offense)
#--gender is men|women (lower case)

#Some confs:
#All: acc,american,atlanticten,bigeast,bigten,bigtwelve,pactwelve,sec,wcc,mountainwest,mvc,conferenceusa,mac,socon,sunbelt,bigsky,colonial,summit,americaeast,atlanticsun,bigsouth,bigwest,horizon,maac,meac,nec,ovc,patriot,southland,swac,wac
#High major women: women_acc,women_american,women_bigeast,women_bigten,women_bigtwelve,women_pactwelve,women_sec,women_misc_conf,misc_conf
export REPLACE_EXIST=${REPLACE_EXIST:="no"}
export YEAR=${YEAR:="2020_21"}

java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
  org.piggottfamily.cbb_explorer.BuildIngestPipeline \
  --out=$PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/ \
  --in=$IN_HTML \
  --gender=$GENDER \
  --confs=$CONFS \
  --year=$YEAR \
  --replace-existing=${REPLACE_EXIST}
