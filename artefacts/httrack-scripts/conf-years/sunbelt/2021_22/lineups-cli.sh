#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=sunbelt
array=(
   '149.0/15881::Coastal+Carolina'
   '254.0/15881::Georgia+St.'
   '702.0/15881::UT+Arlington'
   '646.0/15881::South+Alabama'
   '253.0/15881::Ga.+Southern'
   '30.0/15881::Arkansas+St.'
   '671.0/15881::Louisiana'
   '32.0/15881::Little+Rock'
   '27.0/15881::App+State'
   '670.0/15881::Texas+St.'
   '498.0/15881::ULM'
   '716.0/15881::Troy'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
