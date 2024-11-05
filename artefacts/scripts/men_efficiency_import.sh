#!/bin/bash

TO_UPLOAD=$(ls -t $HOME/Downloads/ | grep -E "summary25[.]csv" | head -n 1)

if [[ "$TO_UPLOAD" != "" ]]; then
   NUM_LINES=$(wc -l $HOME/Downloads/$TO_UPLOAD | awk '{ print $1 }')
   echo "men_efficiency_import: Uploading [$NUM_LINES] teams from [$HOME/Downloads/$TO_UPLOAD]"

   curl "$EFF_TRIGGER_UPLOAD" --data-binary @$HOME/Downloads/$TO_UPLOAD
   echo
   #(remove file so don't spuriously reprocess)
   rm $HOME/Downloads/$TO_UPLOAD

else
   echo "men_efficiency_import: Nothing to upload"
fi