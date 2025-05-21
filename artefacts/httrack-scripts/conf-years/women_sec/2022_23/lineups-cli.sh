#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=women_sec
array=(
   '31.0/16061::Arkansas'
   '648.0/16061::South+Carolina'
   '697.0/16061::Texas+A%26M'
   '8.0/16061::Alabama'
   '736.0/16061::Vanderbilt'
   '434.0/16061::Missouri'
   '334.0/16061::Kentucky'
   '430.0/16061::Mississippi+St.'
   '235.0/16061::Florida'
   '694.0/16061::Tennessee'
   '257.0/16061::Georgia'
   '433.0/16061::Ole+Miss'
   '37.0/16061::Auburn'
   '365.0/16061::LSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
