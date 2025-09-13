#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=sunbelt
array=(
   '149.0/16720::Coastal+Carolina'
   '254.0/16720::Georgia+St.'
   '646.0/16720::South+Alabama'
   '253.0/16720::Ga.+Southern'
   '30.0/16720::Arkansas+St.'
   '671.0/16720::Louisiana'
   '27.0/16720::App+State'
   '670.0/16720::Texas+St.'
   '498.0/16720::ULM'
   '716.0/16720::Troy'
   '388.0/16720::Marshall'
   '523.0/16720::Old+Dominion'
   '664.0/16720::Southern+Miss.'
   '317.0/16720::James+Madison'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"