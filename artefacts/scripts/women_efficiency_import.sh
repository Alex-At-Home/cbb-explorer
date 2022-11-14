#!/bin/bash

TO_UPLOAD=$(ls -t $HOME/Downloads/export-[0-9]*.csv $HOME/Downloads/export.csv | head -n 1)

if [[ "$TO_UPLOAD" != "" ]]; then
   echo "women_efficiency_import: Uploading [$TO_UPLOAD]"

   curl "$EFF_WOMEN_TRIGGER_UPLOAD" --data-binary @$TO_UPLOAD

   echo "women_efficiency_import: Deleting [$(ls -t $HOME/Downloads/export-[0-9]*.csv $HOME/Downloads/export.csv | wc -l)] old files"
   for i in $(ls -t $HOME/Downloads/export-[0-9]*.csv $HOME/Downloads/export.csv); do
   rm $i
   done
else
   echo "women_efficiency_import: Nothing to upload"
fi