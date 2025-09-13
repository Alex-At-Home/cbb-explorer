#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=americaeast
array=(
   '738.0/15866::Vermont'
   '683.0/15866::Stony+Brook'
   '391.0/15866::UMBC'
   '14.0/15866::Albany+%28NY%29'
   '368.0/15866::UMass+Lowell'
   '469.0/15866::New+Hampshire'
   '471.0/15866::NJIT'
   '272.0/15866::Hartford'
   '62.0/15866::Binghamton'
   '380.0/15866::Maine'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
