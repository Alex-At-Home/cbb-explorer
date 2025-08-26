#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=sec
array=(
   '365.0/15480::LSU'
   '31.0/15480::Arkansas'
   '8.0/15480::Alabama'
   '257.0/15480::Georgia'
   '694.0/15480::Tennessee'
   '235.0/15480::Florida'
   '37.0/15480::Auburn'
   '430.0/15480::Mississippi+St.'
   '736.0/15480::Vanderbilt'
   '648.0/15480::South+Carolina'
   '433.0/15480::Ole+Miss'
   '434.0/15480::Missouri'
   '334.0/15480::Kentucky'
   '697.0/15480::Texas+A%26M'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
