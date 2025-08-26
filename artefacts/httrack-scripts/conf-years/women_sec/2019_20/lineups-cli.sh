#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=women_sec
array=(
   '31.0/15002::Arkansas'
   '648.0/15002::South+Carolina'
   '430.0/15002::Mississippi+St.'
   '334.0/15002::Kentucky'
   '8.0/15002::Alabama'
   '694.0/15002::Tennessee'
   '697.0/15002::Texas+A%26M'
   '37.0/15002::Auburn'
   '736.0/15002::Vanderbilt'
   '434.0/15002::Missouri'
   '365.0/15002::LSU'
   '235.0/15002::Florida'
   '257.0/15002::Georgia'
   '433.0/15002::Ole+Miss'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
