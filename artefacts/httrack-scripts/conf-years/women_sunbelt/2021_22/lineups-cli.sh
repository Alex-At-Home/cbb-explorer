#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=sunbelt
array=(
   '149.0/15866::Coastal+Carolina'
   '254.0/15866::Georgia+St.'
   '702.0/15866::UT+Arlington'
   '646.0/15866::South+Alabama'
   '253.0/15866::Ga.+Southern'
   '30.0/15866::Arkansas+St.'
   '671.0/15866::Louisiana'
   '32.0/15866::Little+Rock'
   '27.0/15866::App+State'
   '670.0/15866::Texas+St.'
   '498.0/15866::ULM'
   '716.0/15866::Troy'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
