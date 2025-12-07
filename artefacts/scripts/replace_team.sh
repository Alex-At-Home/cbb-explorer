#!/bin/bash
# TEAM_NAME TEAM_NAME_URL CONF (eg swac) CURR_YEAR_STR
# CURR_TIME to a 1-up number, eg 12, 13 etc
# optionally set REDOWNLOAD="yes" to redownload
# optionally set REPROCESS="no" to skip the parsing (eg if you are going to do it later)
# DRY_RUN=yes to ignore
# eg PING="lpong" CURR_TIME="12" TEAM_NAME="Alabama A&M" TEAM_NAME_URL="Alabama+A%26M" CURR_YEAR_STR="2021_22" CONF="swac" DRY_RUN="yes" 

REPROCESS=${REPROCESS:="yes"}
REDOWNLOAD=${REDOWNLOAD:="no"}
CURR_YEAR_STR=${CURR_YEAR_STR:="2024_25"}
CURR_YEAR=$(echo $CURR_YEAR_STR | cut -c1-4)

if [[ "$REDOWNLOAD" == "yes" ]] && [[ "$CURR_YEAR_STR" == "" ]]; then
    echo "If re-downloading must specify CURR_YEAR_STR"
fi
if [[ "$REDOWNLOAD" == "yes" ]] && [[ "$TEAM_NAME_URL" == "" ]]; then
    echo "If re-downloading must specify TEAM_NAME_URL"
fi

if [[ "$REPROCESS" == "yes" ]]; then
    if [[ "$TEAM_NAME" == "" ]] || [[ "$CONF" == "" ]] || [[ "$CURR_TIME" == "" ]] || [[ "$CURR_YEAR_STR" == "" ]] || [[ "$PING" == "" ]]; then
        echo "Must specify TEAM_NAME [${TEAM_NAME}] and CONF [${CONF}] and CURR_YEAR_STR [${CURR_YEAR_STR}] and CURR_TIME [${CURR_TIME}] and PING [${PING}]"
        exit -1
    fi
else
    if [[ "$TEAM_NAME" == "" ]]; then
        echo "Must specify TEAM_NAME [${TEAM_NAME}]"
        exit -1
    fi
fi

if [[ $CONF =~ women.* ]]; then
    MAYBE_MEN=""
    GENDER="women"
else
    MAYBE_MEN="_men"
    GENDER="men"
fi


echo "Removing team [$TEAM_NAME]/[$TEAM_NAME_URL] (conf: [$CONF], year: [$CURR_YEAR] from DB: (DRY_RUN=[$DRY_RUN]))"
if [[ "$DRY_RUN" != "yes" ]]; then

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

    curl -XPOST -H 'Content-Type: application/json' -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/shot_events${MAYBE_MEN}_${CONF}_${CURR_YEAR}/_delete_by_query" -d "{
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

    if [[ "$REPROCESS" == "yes" ]]; then
        echo "Re-uploading data"

        if [[ "$REDOWNLOAD" == "yes" ]]; then
            echo "(includes download)"
            PING="$PING" CURR_TIME="$CURR_TIME" DOWNLOAD="$REDOWNLOAD" PARSE="yes" UPLOAD="yes" CURR_YEAR_STR="$CURR_YEAR_STR" TEAM_URL_FILTER="$TEAM_NAME_URL" CONFS="$CONF" CLOSE_EOF=true $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
        else
            PING="$PING" CURR_TIME="$CURR_TIME" DOWNLOAD="$REDOWNLOAD" PARSE="yes" UPLOAD="yes" CURR_YEAR_STR="$CURR_YEAR_STR" TEAM_FILTER="$TEAM_NAME" CONFS="$CONF" CLOSE_EOF=true $PBP_SRC_ROOT/artefacts/scripts/bulk_lineup_import.sh
        fi 
    fi
else
    echo "(Dry Run)"
    echo "URL1: https://$ELASTIC_URL/${CONF}_${CURR_YEAR}/_delete_by_query"
    echo "URL2: https://$ELASTIC_URL/player_events${MAYBE_MEN}_${CONF}_${CURR_YEAR}/_delete_by_query"
    echo "URL2: https://$ELASTIC_URL/shot_events${MAYBE_MEN}_${CONF}_${CURR_YEAR}/_delete_by_query"
    echo "URL3: https://$ELASTIC_URL/bad_lineups_${GENDER}_$CURR_YEAR/_delete_by_query"
    echo "Team Name: [$TEAM_NAME]"
    if [[ "$REPROCESS" == "yes" ]]; then
        if [[ "$REDOWNLOAD" == "yes" ]]; then
            echo "PING=\"$PING\" CURR_TIME=\"$CURR_TIME\" DOWNLOAD=\"$REDOWNLOAD\" PARSE=\"yes\" UPLOAD=\"yes\" CURR_YEAR_STR=\"$CURR_YEAR_STR\" TEAM_URL_FILTER=\"$TEAM_NAME_URL\" CONFS=\"$CONF\""
        else
            echo "PING=\"$PING\" CURR_TIME=\"$CURR_TIME\" DOWNLOAD=\"$REDOWNLOAD\" PARSE=\"yes\" UPLOAD=\"yes\" CURR_YEAR_STR=\"$CURR_YEAR_STR\" TEAM_FILTER=\"$TEAM_NAME\" CONFS=\"$CONF\""
        fi
    fi
fi