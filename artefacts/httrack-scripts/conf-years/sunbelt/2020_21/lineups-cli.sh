#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=sunbelt
array=(
   '149.0/15480::Coastal+Carolina'
   '254.0/15480::Georgia+St.'
   '702.0/15480::UT+Arlington'
   '646.0/15480::South+Alabama'
   '253.0/15480::Ga.+Southern'
   '30.0/15480::Arkansas+St.'
   '671.0/15480::Louisiana'
   '32.0/15480::Little+Rock'
   '27.0/15480::App+State'
   '670.0/15480::Texas+St.'
   '498.0/15480::ULM'
   '716.0/15480::Troy'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
