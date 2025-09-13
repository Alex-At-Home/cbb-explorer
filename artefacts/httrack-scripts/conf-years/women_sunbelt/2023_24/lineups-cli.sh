#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=sunbelt
array=(
   '149.0/16500::Coastal+Carolina'
   '254.0/16500::Georgia+St.'
   '646.0/16500::South+Alabama'
   '253.0/16500::Ga.+Southern'
   '30.0/16500::Arkansas+St.'
   '671.0/16500::Louisiana'
   '27.0/16500::App+State'
   '670.0/16500::Texas+St.'
   '498.0/16500::ULM'
   '716.0/16500::Troy'
   '388.0/16500::Marshall'
   '523.0/16500::Old+Dominion'
   '664.0/16500::Southern+Miss.'
   '317.0/16500::James+Madison'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"