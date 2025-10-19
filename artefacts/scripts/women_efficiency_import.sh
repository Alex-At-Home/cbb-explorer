#!/bin/bash

TO_DOWNLOAD=$HOME/Downloads/2026_team_results.csv
rm -f $TO_DOWNLOAD
curl -o $TO_DOWNLOAD 'https://barttorvik.com/ncaaw/2026_team_results.csv'

TO_UPLOAD=$(ls -t $HOME/Downloads/ | grep -E "^2026_team_results[.]csv" | head -n 1)
if [[ "$TO_UPLOAD" != "" ]]; then
   NUM_LINES=$(wc -l $HOME/Downloads/$TO_UPLOAD | awk '{ print $1 }')
   echo "women_efficiency_import: Uploading [$NUM_LINES] teams from [$HOME/Downloads/$TO_UPLOAD]"

   curl "$EFF_WOMEN_TRIGGER_UPLOAD" --data-binary @$HOME/Downloads/$TO_UPLOAD
   echo
else
   echo "women_efficiency_import: Nothing to upload"
fi