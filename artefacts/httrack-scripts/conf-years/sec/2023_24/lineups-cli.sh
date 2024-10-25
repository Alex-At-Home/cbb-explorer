#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=sec
array=(
   '365.0/16501::LSU'
   '31.0/16501::Arkansas'
   '8.0/16501::Alabama'
   '257.0/16501::Georgia'
   '694.0/16501::Tennessee'
   '235.0/16501::Florida'
   '37.0/16501::Auburn'
   '430.0/16501::Mississippi+St.'
   '736.0/16501::Vanderbilt'
   '648.0/16501::South+Carolina'
   '433.0/16501::Ole+Miss'
   '434.0/16501::Missouri'
   '334.0/16501::Kentucky'
   '697.0/16501::Texas+A%26M'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
