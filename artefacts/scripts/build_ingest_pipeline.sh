#!/bin/bash

echo "NO LONGER USED - SEE build_another_year_httrack_script.sh"
echo "To build AvailableTeams in cbb-on-off-analyzer, see AvailableTeams.byName docs"
exit 1

# mid majors:
#CONFS="wcc,mountainwest,mvc,conferenceusa,mac,socon,sunbelt,bigsky,colonial,summit"

#--in comes from "https://stats.ncaa.org/rankings/change_sport_year_div" (basketball > year > DI > Team > Scoring Offense)
#--gender is men|women (lower case)

#Some confs:
#All: acc,american,atlanticten,bigeast,bigten,bigtwelve,pactwelve,sec,wcc,mountainwest,mvc,conferenceusa,mac,socon,sunbelt,bigsky,colonial,summit,americaeast,atlanticsun,bigsouth,bigwest,horizon,ivy,maac,meac,nec,ovc,patriot,southland,swac,wac
#High major women: acc,american,bigeast,bigten,bigtwelve,pactwelve,sec
#(misc_conf not currently supported)

export REPLACE_EXIST=${REPLACE_EXIST:="no"}
export YEAR=${YEAR:="2023_24"}
if [ "$GENDER" == "men" ]; then
  export CONFS={CONFS:="acc,american,atlanticten,bigeast,bigten,bigtwelve,pactwelve,sec,wcc,mountainwest,mvc,conferenceusa,mac,socon,sunbelt,bigsky,colonial,summit,americaeast,atlanticsun,bigsouth,bigwest,horizon,ivy,maac,meac,nec,ovc,patriot,southland,swac,wac"}
elif [ "$GENDER" == "women" ]; then
  export CONFS={CONFS:="acc,american,bigeast,bigten,bigtwelve,pactwelve,sec"}
else
  echo "Need GENDER=men|women"
  exit 1
fi

java -cp "$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer-assembly-0.1-deps.jar:$PBP_SRC_ROOT/target/scala-2.12/cbb-explorer_2.12-0.1.jar" \
  org.piggottfamily.cbb_explorer.BuildIngestPipeline \
  --out=$PBP_SRC_ROOT/artefacts/httrack-scripts/conf-years/ \
  --in=$IN_HTML \
  --gender=$GENDER \
  --confs=$CONFS \
  --year=$YEAR \
  --replace-existing=${REPLACE_EXIST}
