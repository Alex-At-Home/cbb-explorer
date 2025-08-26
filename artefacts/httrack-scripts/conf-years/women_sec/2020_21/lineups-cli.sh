#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=women_sec
array=(
   '31.0/15500::Arkansas'
   '648.0/15500::South+Carolina'
   '697.0/15500::Texas+A%26M'
   '8.0/15500::Alabama'
   '736.0/15500::Vanderbilt'
   '434.0/15500::Missouri'
   '334.0/15500::Kentucky'
   '430.0/15500::Mississippi+St.'
   '235.0/15500::Florida'
   '694.0/15500::Tennessee'
   '257.0/15500::Georgia'
   '433.0/15500::Ole+Miss'
   '37.0/15500::Auburn'
   '365.0/15500::LSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
