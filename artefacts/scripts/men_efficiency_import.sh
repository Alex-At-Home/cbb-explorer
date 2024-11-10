#!/bin/bash

#(the URL is downloaded at 3a via Automator, see artefacts/macos-scripts/efficiency-download, duplicate as a calendar alarm)

TO_UPLOAD=$(ls -t $HOME/Downloads/ | grep -E "summary25[.]csv" | head -n 1)

if [[ "$TO_UPLOAD" != "" ]]; then
   NUM_LINES=$(wc -l $HOME/Downloads/$TO_UPLOAD | awk '{ print $1 }')
   echo "men_efficiency_import: Uploading [$NUM_LINES] teams from [$HOME/Downloads/$TO_UPLOAD]"

   curl "$EFF_TRIGGER_UPLOAD" --data-binary @$HOME/Downloads/$TO_UPLOAD
   echo
   #(remove file so don't spuriously reprocess)
   rm -f $HOME/Downloads/last_$TO_UPLOAD
   mv $HOME/Downloads/$TO_UPLOAD $HOME/Downloads/last_$TO_UPLOAD

else
   echo "men_efficiency_import: Nothing to upload"
fi