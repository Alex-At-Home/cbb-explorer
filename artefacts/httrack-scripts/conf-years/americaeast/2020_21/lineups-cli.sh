#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=americaeast
array=(
   '738.0/15480::Vermont'
   '683.0/15480::Stony+Brook'
   '391.0/15480::UMBC'
   '14.0/15480::Albany+%28NY%29'
   '368.0/15480::UMass+Lowell'
   '469.0/15480::New+Hampshire'
   '471.0/15480::NJIT'
   '272.0/15480::Hartford'
   '62.0/15480::Binghamton'
   '380.0/15480::Maine'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
