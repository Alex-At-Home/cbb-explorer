# WON'T WORK IN PRACTICE - RETAINED FOR HISTORICAL PURPOSES
# SEE artefacts/scripts/full_men_efficiency_import.sh

# Clears memory of all files processed
#to restore a specific saved file state:
#cp saved_registry.json filebeat-6.5.4-darwin-x86_64/data/registry/filebeat/data.json
rm -f filebeat-7.3.0-darwin-x86_64/data/registry/filebeat/data.json

# Copy the last file generated to one of the files filebeat looks for
cp ~/.cbb-explorer/.teams eff_stats.ndjson

# Run filebeat
./filebeat-7.3.0-darwin-x86_64/filebeat -c filebeat_eff.yaml

#(now delete any files you don't want to re-upload)
