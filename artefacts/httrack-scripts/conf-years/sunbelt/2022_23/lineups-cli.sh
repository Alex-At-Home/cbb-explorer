#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=sunbelt
array=(
   '149.0/16060::Coastal+Carolina'
   '254.0/16060::Georgia+St.'
   '646.0/16060::South+Alabama'
   '253.0/16060::Ga.+Southern'
   '30.0/16060::Arkansas+St.'
   '671.0/16060::Louisiana'
   '27.0/16060::App+State'
   '670.0/16060::Texas+St.'
   '498.0/16060::ULM'
   '716.0/16060::Troy'
   '388.0/16060::Marshall'
   '523.0/16060::Old+Dominion'
   '664.0/16060::Southern+Miss.'
   '317.0/16060::James+Madison'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
