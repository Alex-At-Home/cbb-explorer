#!/bin/bash

TO_UPLOAD=$(ls -t $HOME/Downloads/ | grep -E "export(-[0-9]*)?[.]csv" | head -n 1)

if [[ "$TO_UPLOAD" != "" ]]; then
   NUM_LINES=$(wc -l $HOME/Downloads/$TO_UPLOAD | awk '{ print $1 }')
   echo "women_efficiency_import: Uploading [$NUM_LINES] teams from [$HOME/Downloads/$TO_UPLOAD]"

   curl "$EFF_WOMEN_TRIGGER_UPLOAD" --data-binary @$HOME/Downloads/$TO_UPLOAD
   echo

   echo "women_efficiency_import: Deleting [$(ls -t $HOME/Downloads/ | grep -E "export(-[0-9]*)?[.]csv" | wc -l)] old files"
   for i in $(ls -t $HOME/Downloads/ | grep -E "export(-[0-9]*)?[.]csv"); do
      echo "women_efficiency_import: deleting [$HOME/Downloads/$i]"
      rm $HOME/Downloads/$i
   done
else
   echo "women_efficiency_import: Nothing to upload"
fi