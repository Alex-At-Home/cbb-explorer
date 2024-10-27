#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=sunbelt
array=(
   '149.0/16501::Coastal+Carolina'
   '254.0/16501::Georgia+St.'
   '646.0/16501::South+Alabama'
   '253.0/16501::Ga.+Southern'
   '30.0/16501::Arkansas+St.'
   '671.0/16501::Louisiana'
   '27.0/16501::App+State'
   '670.0/16501::Texas+St.'
   '498.0/16501::ULM'
   '716.0/16501::Troy'
   '388.0/16501::Marshall'
   '523.0/16501::Old+Dominion'
   '664.0/16501::Southern+Miss.'
   '317.0/16501::James+Madison'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"