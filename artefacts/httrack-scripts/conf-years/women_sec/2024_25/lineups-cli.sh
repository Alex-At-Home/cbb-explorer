#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=women_sec
array=(
   '31.0/16720::Arkansas'
   '648.0/16720::South+Carolina'
   '697.0/16720::Texas+A%26M'
   '8.0/16720::Alabama'
   '736.0/16720::Vanderbilt'
   '434.0/16720::Missouri'
   '334.0/16720::Kentucky'
   '430.0/16720::Mississippi+St.'
   '235.0/16720::Florida'
   '694.0/16720::Tennessee'
   '257.0/16720::Georgia'
   '433.0/16720::Ole+Miss'
   '37.0/16720::Auburn'
   '365.0/16720::LSU'
   '522.0/16720::Oklahoma'
   '703.0/16720::Texas'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"