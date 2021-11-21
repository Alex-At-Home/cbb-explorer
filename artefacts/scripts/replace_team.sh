#!/bin/bash
# TEAM_NAME TEAM_NAME_URL CONF (eg swac) CURR_YEAR_STR
# CURR_TIME to a 1-up number, eg 12, 13 etc
# optionally set REDOWNLOAD to redownload
# DRY_RUN=yes to ignore
# eg PING="lpong" CURR_TIME="12" TEAM_NAME="Alabama A&M" TEAM_NAME_URL="Alabama+A%26M" CURR_YEAR_STR="2021_22" CONF_YEAR="swac" DRY_RUN="yes" 

REDOWNLOAD=${REDOWNLOAD:="no"}
CURR_YEAR_STR=${CURR_YEAR_STR:="2021_22"}
CURR_YEAR=$(echo $CURR_YEAR_STR | cut -c1-4)

if [[ "$REDOWNLOAD" == "yes" ]] && [[ "$CURR_YEAR_STR" == "" ]]; then
    echo "If re-downloading must specify CURR_YEAR_STR"
fi

if [[ "$TEAM_NAME" == "" ]] || [[ "$TEAM_NAME_URL" == "" ]] || [[ "$CONF" == "" ]] || [[ "$CURR_TIME" == "" ]] || [[ "$CURR_YEAR_STR" == "" ]] || [[ "$PING" == "" ]]; then
    echo "Must specify TEAM_NAME [${TEAM_NAME}] and TEAM_NAME_URL [${TEAM_NAME_URL}] and CONF [${CONF}] and CURR_YEAR_STR [${CURR_YEAR_STR}] and CURR_TIME [${CURR_TIME}] and PING [${PING}]"
    exit -1
fi

if [[ $CONF =~ women.* ]]; then
    MAYBE_MEN=""
    GENDER="women"
else
    MAYBE_MEN="_men"
    GENDER="men"
fi


if [[ "$DRY_RUN" != "yes" ]]; then
    echo "Removing team [$TEAM_NAME]/[$TEAM_NAME_URL] (conf: [$CONF], year: [$CURR_YEAR] from DB:"

    curl -XPOST -H 'Content-Type: application/json' -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/${CONF}_${CURR_YEAR}/_delete_by_query" -d "{
    \"query\": {
        \"term\": {
        \"team.team.keyword\": {
            \"value\": \"$TEAM_NAME\"
        }
        }
    }
    }"

    curl -XPOST -H 'Content-Type: application/json' -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/player_events${MAYBE_MEN}_${CONF}_${CURR_YEAR}/_delete_by_query" -d "{
    \"query\": {
        \"term\": {
        \"team.team.keyword\": {
            \"value\": \"$TEAM_NAME\"
        }
        }
    }
    }"

    curl -XPOST -H 'Content-Type: application/json' -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/bad_lineups_${GENDER}_$CURR_YEAR/_delete_by_query" -d "{
    \"query\": {
        \"term\": {
        \"team.team.keyword\": {
            \"value\": \"$TEAM_NAME\"
        }
        }
    }
    }"

    echo "Re-uploading data"

    PING="$PING" CURR_TIME="$CURR_TIME" DOWNLOAD="$REDOWNLOAD" PARSE="yes" UPLOAD="yes" CURR_YEAR_STR="$CURR_YEAR_STR" TEAM_FILTER="=$TEAM_NAME_URL" CONFS="$CONF" $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh 

else
    echo "(Dry Run)"
    echo "URL1: https://$ELASTIC_URL/${CONF}_${CURR_YEAR}/_delete_by_query"
    echo "URL2: https://$ELASTIC_URL/player_events${MAYBE_MEN}_${CONF}_${CURR_YEAR}/_delete_by_query"
    echo "URL3: https://$ELASTIC_URL/bad_lineups_${GENDER}_$CURR_YEAR/_delete_by_query"
    echo "PING=\"$PING\" CURR_TIME=\"$CURR_TIME\" DOWNLOAD=\"$REDOWNLOAD\" PARSE=\"yes\" UPLOAD=\"yes\" CURR_YEAR_STR=\"$CURR_YEAR_STR\" TEAM_FILTER=\"=$TEAM_NAME_URL\" CONFS=\"$CONF\""
fi