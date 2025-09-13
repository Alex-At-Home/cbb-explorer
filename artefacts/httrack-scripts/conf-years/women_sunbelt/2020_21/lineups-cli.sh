#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=sunbelt
array=(
   '149.0/15500::Coastal+Carolina'
   '254.0/15500::Georgia+St.'
   '702.0/15500::UT+Arlington'
   '646.0/15500::South+Alabama'
   '253.0/15500::Ga.+Southern'
   '30.0/15500::Arkansas+St.'
   '671.0/15500::Louisiana'
   '32.0/15500::Little+Rock'
   '27.0/15500::App+State'
   '670.0/15500::Texas+St.'
   '498.0/15500::ULM'
   '716.0/15500::Troy'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
