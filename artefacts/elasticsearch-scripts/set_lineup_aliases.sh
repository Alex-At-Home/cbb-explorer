#!/bin/bash

if [ "$PING" != "lping" ] && [ "$PING" != "lpong" ]; then
  echo "Need to specify PING env var as either 'lping' or 'lpong', not '$PING'"
  exit -1
fi
if [ "$PING" = "lping" ]; then
   PONG="lpong"
else
  PONG="lping"
fi
if [ "$YEAR" == "" ]; then
  YEAR="20[1-9][0-9]"
fi

export INDICES=$(curl -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/_cat/indices"| grep -E "_${YEAR}(_lping|_lpong|\\$)?" | awk  '{ print $3 }' | sort -u)

for INDEX in $INDICES; do
  ROOT=$(echo "$INDEX" | sed -E s/'_(lping|lpong)'/''/)
  echo ""
  echo "Updating alias for $ROOT from $PONG to $PING"
  INDEX_TO="${ROOT}_${PING}"
  INDEX_FROM="${ROOT}_${PONG}"
  ALIAS="${ROOT}"

  if [ $BUILD_ONLY="yes" ]; then
    #(just adds the alias to the specified PING)
    curl -XPOST -H 'Content-Type: application/json' -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/_aliases" -d "{
      \"actions\": [
        { \"add\": { \"index\": \"$INDEX_TO\", \"alias\": \"$ALIAS\" } }
      ]
    }"
  else
    # Removes PONG and adds PING
    curl -XPOST -H 'Content-Type: application/json' -u "$ELASTIC_USER:$ELASTIC_PASS" "https://$ELASTIC_URL/_aliases" -d "{
      \"actions\": [
        { \"add\": { \"index\": \"$INDEX_TO\", \"alias\": \"$ALIAS\" } },
        { \"remove\": { \"index\": \"$INDEX_FROM\", \"alias\": \"$ALIAS\" } }
      ]
    }"
  fi
done
