#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=americaeast
array=(
   '738.0/16061::Vermont'
   '391.0/16061::UMBC'
   '14.0/16061::UAlbany'
   '368.0/16061::UMass+Lowell'
   '469.0/16061::New+Hampshire'
   '471.0/16061::NJIT'
   '272.0/16061::Hartford'
   '62.0/16061::Binghamton'
   '380.0/16061::Maine'
   '81.0/16061::Bryant'
   '682.0/16061::Stonehill'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
