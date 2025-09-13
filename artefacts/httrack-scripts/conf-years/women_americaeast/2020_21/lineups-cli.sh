#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=americaeast
array=(
   '738.0/15500::Vermont'
   '683.0/15500::Stony+Brook'
   '391.0/15500::UMBC'
   '14.0/15500::Albany+%28NY%29'
   '368.0/15500::UMass+Lowell'
   '469.0/15500::New+Hampshire'
   '471.0/15500::NJIT'
   '272.0/15500::Hartford'
   '62.0/15500::Binghamton'
   '380.0/15500::Maine'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
