#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=americaeast
array=(
   '738.0/16720::Vermont'
   '391.0/16720::UMBC'
   '14.0/16720::UAlbany'
   '368.0/16720::UMass+Lowell'
   '469.0/16720::New+Hampshire'
   '471.0/16720::NJIT'
   '62.0/16720::Binghamton'
   '380.0/16720::Maine'
   '81.0/16720::Bryant'
   '682.0/16720::Stonehill'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"