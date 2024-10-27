#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=sunbelt
array=(
   '149.0/16700::Coastal+Carolina'
   '254.0/16700::Georgia+St.'
   '646.0/16700::South+Alabama'
   '253.0/16700::Ga.+Southern'
   '30.0/16700::Arkansas+St.'
   '671.0/16700::Louisiana'
   '27.0/16700::App+State'
   '670.0/16700::Texas+St.'
   '498.0/16700::ULM'
   '716.0/16700::Troy'
   '388.0/16700::Marshall'
   '523.0/16700::Old+Dominion'
   '664.0/16700::Southern+Miss.'
   '317.0/16700::James+Madison'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"