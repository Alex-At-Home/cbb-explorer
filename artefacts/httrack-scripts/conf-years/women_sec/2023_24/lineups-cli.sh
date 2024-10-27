#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=women_sec
array=(
   '31.0/16500::Arkansas'
   '648.0/16500::South+Carolina'
   '697.0/16500::Texas+A%26M'
   '8.0/16500::Alabama'
   '736.0/16500::Vanderbilt'
   '434.0/16500::Missouri'
   '334.0/16500::Kentucky'
   '430.0/16500::Mississippi+St.'
   '235.0/16500::Florida'
   '694.0/16500::Tennessee'
   '257.0/16500::Georgia'
   '433.0/16500::Ole+Miss'
   '37.0/16500::Auburn'
   '365.0/16500::LSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"