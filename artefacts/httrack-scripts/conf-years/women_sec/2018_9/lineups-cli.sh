#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=women_sec
array=(
   '430.0/14320::Mississippi+St.'
   '648.0/14320::South+Carolina'
   '694.0/14320::Tennessee'
   '31.0/14320::Arkansas'
   '37.0/14320::Auburn'
   '334.0/14320::Kentucky'
   '697.0/14320::Texas+A%26M'
   '257.0/14320::Georgia'
   '434.0/14320::Missouri'
   '736.0/14320::Vanderbilt'
   '8.0/14320::Alabama'
   '365.0/14320::LSU'
   '235.0/14320::Florida'
   '433.0/14320::Ole+Miss'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
