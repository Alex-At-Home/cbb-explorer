#!/bin/bash

TO_UPLOAD=$(ls -t $HOME/Downloads/export-[0-9]*.csv $HOME/Downloads/export.csv | head -n 1)

if [[ "$TO_UPLOAD" != "" ]]; then
   NUM_LINES=$(wc -l $TO_UPLOAD | awk '{ print $1 }')
   echo "women_efficiency_import: Uploading [$NUM_LINES] teams from [$TO_UPLOAD]"

   curl "$EFF_WOMEN_TRIGGER_UPLOAD" --data-binary @$TO_UPLOAD
   echo

   echo "women_efficiency_import: Deleting [$(ls -t $HOME/Downloads/export-[0-9]*.csv $HOME/Downloads/export.csv | wc -l)] old files"
   for i in $(ls -t $HOME/Downloads/export-[0-9]*.csv $HOME/Downloads/export.csv); do
      echo "women_efficiency_import: deleting [$i]"
      rm $i
   done
else
   echo "women_efficiency_import: Nothing to upload"
fi