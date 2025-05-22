#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=sec
array=(
   '365.0/15881::LSU'
   '31.0/15881::Arkansas'
   '8.0/15881::Alabama'
   '257.0/15881::Georgia'
   '694.0/15881::Tennessee'
   '235.0/15881::Florida'
   '37.0/15881::Auburn'
   '430.0/15881::Mississippi+St.'
   '736.0/15881::Vanderbilt'
   '648.0/15881::South+Carolina'
   '433.0/15881::Ole+Miss'
   '434.0/15881::Missouri'
   '334.0/15881::Kentucky'
   '697.0/15881::Texas+A%26M'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
