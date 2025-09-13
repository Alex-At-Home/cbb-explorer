#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=americaeast
array=(
   '738.0/16500::Vermont'
   '391.0/16500::UMBC'
   '14.0/16500::UAlbany'
   '368.0/16500::UMass+Lowell'
   '469.0/16500::New+Hampshire'
   '471.0/16500::NJIT'
   '62.0/16500::Binghamton'
   '380.0/16500::Maine'
   '81.0/16500::Bryant'
   '682.0/16500::Stonehill'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"