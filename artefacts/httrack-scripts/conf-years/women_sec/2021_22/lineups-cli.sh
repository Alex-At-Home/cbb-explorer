#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=women_sec
array=(
   '31.0/15866::Arkansas'
   '648.0/15866::South+Carolina'
   '697.0/15866::Texas+A%26M'
   '8.0/15866::Alabama'
   '736.0/15866::Vanderbilt'
   '434.0/15866::Missouri'
   '334.0/15866::Kentucky'
   '430.0/15866::Mississippi+St.'
   '235.0/15866::Florida'
   '694.0/15866::Tennessee'
   '257.0/15866::Georgia'
   '433.0/15866::Ole+Miss'
   '37.0/15866::Auburn'
   '365.0/15866::LSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
