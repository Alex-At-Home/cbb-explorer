#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=sec
array=(
   '365.0/16700::LSU'
   '31.0/16700::Arkansas'
   '8.0/16700::Alabama'
   '257.0/16700::Georgia'
   '694.0/16700::Tennessee'
   '235.0/16700::Florida'
   '37.0/16700::Auburn'
   '430.0/16700::Mississippi+St.'
   '736.0/16700::Vanderbilt'
   '648.0/16700::South+Carolina'
   '433.0/16700::Ole+Miss'
   '434.0/16700::Missouri'
   '334.0/16700::Kentucky'
   '697.0/16700::Texas+A%26M'
   '522.0/16700::Oklahoma'
   '703.0/16700::Texas'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
