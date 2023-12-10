#!/bin/bash

# Deletes data for (likelt a team), eg 
#DRY_RUN="no" GENDER="men" YEAR=2021 CONF=bigtwelve QUERY="team.team:TCU" sh $PBP_SRC_ROOT/artefacts/elasticsearch-scripts/delete_data.sh

if [ "$YEAR" == "" ]; then
  echo "Need to specify YEAR, CONF, QUERY, DRY_RUN ([YEAR])"
  exit -1
fi
if [ "$CONF" == "" ]; then
  echo "Need to specify YEAR, CONF, QUERY, DRY_RUN ([CONF])"
  exit -1
fi
if [ "$QUERY" == "" ]; then
  echo "Need to specify YEAR, CONF, QUERY, DRY_RUN ([QUERY])"
  echo "Example query: 'team.team:NAME' or 'team.team:*'"
  exit -1
fi
GENDER=${GENDER:-"men"}
if [ "$GENDER" != "men" ] && [ "$GENDER" != "women" ]; then
  echo "If specified, GENDER needs to be men or women, not [$GENDER]"
  exit -1
fi
if [ "$DRY_RUN" == "" ]; then
  echo "Need to specify YEAR, CONF, QUERY, DRY_RUN ([DRY_RUN])"
  exit -1
fi
if [ "$DRY_RUN" != "yes" ] && [ "$DRY_RUN" != "no" ]; then
  echo "DRY_RUN needs to be yes or no, not [$DRY_RUN]"
  exit -1
fi


if [ "$GENDER" == "women" ]; then
   TEAM_INDEX="women_${CONF}_$YEAR"
else
   TEAM_INDEX="${CONF}_$YEAR"
fi
PLAYER_INDEX="player_events_${GENDER}_${CONF}_$YEAR"
BAD_INDEX="bad_lineups_${GENDER}_$YEAR"

echo "Deleting [$QUERY] from 3 indices: [$TEAM_INDEX]/[$PLAYER_INDEX]/[$BAD_INDEX]"

QUERY="{ \"query\": { \"query_string\": { \"query\": \"$QUERY\" } } }"

for i in "$TEAM_INDEX" "$PLAYER_INDEX" "$BAD_INDEX"; do
   echo "Index: [$i]"
   if [ "$DRY_RUN" == "no" ]; then
      echo "(for real)"
      curl -H 'content-type: application/json' -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/$i/_delete_by_query" -d "$QUERY"
   else
      echo "(dry run)"
      curl -H 'content-type: application/json' -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/$i/_search?size=0" -d "$QUERY"
   fi
done
