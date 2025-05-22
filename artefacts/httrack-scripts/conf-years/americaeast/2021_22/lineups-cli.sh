#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=americaeast
array=(
   '738.0/15881::Vermont'
   '683.0/15881::Stony+Brook'
   '391.0/15881::UMBC'
   '14.0/15881::Albany+%28NY%29'
   '368.0/15881::UMass+Lowell'
   '469.0/15881::New+Hampshire'
   '471.0/15881::NJIT'
   '272.0/15881::Hartford'
   '62.0/15881::Binghamton'
   '380.0/15881::Maine'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
