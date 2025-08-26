#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=sec
array=(
   '8.0/15061::Alabama'
   '365.0/15061::LSU'
   '37.0/15061::Auburn'
   '257.0/15061::Georgia'
   '31.0/15061::Arkansas'
   '334.0/15061::Kentucky'
   '430.0/15061::Mississippi+St.'
   '648.0/15061::South+Carolina'
   '235.0/15061::Florida'
   '736.0/15061::Vanderbilt'
   '433.0/15061::Ole+Miss'
   '434.0/15061::Missouri'
   '694.0/15061::Tennessee'
   '697.0/15061::Texas+A%26M'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
