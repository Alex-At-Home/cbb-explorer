#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=sec
array=(
   '694.0/14300::Tennessee'
   '365.0/14300::LSU'
   '37.0/14300::Auburn'
   '430.0/14300::Mississippi+St.'
   '334.0/14300::Kentucky'
   '433.0/14300::Ole+Miss'
   '31.0/14300::Arkansas'
   '648.0/14300::South+Carolina'
   '8.0/14300::Alabama'
   '257.0/14300::Georgia'
   '697.0/14300::Texas+A%26M'
   '736.0/14300::Vanderbilt'
   '235.0/14300::Florida'
   '434.0/14300::Missouri'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
