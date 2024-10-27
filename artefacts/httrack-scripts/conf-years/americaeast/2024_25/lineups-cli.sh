#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=americaeast
array=(
   '738.0/16700::Vermont'
   '391.0/16700::UMBC'
   '14.0/16700::UAlbany'
   '368.0/16700::UMass+Lowell'
   '469.0/16700::New+Hampshire'
   '471.0/16700::NJIT'
   '62.0/16700::Binghamton'
   '380.0/16700::Maine'
   '81.0/16700::Bryant'
   '682.0/16700::Stonehill'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"