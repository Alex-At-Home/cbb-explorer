#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=bigtwelve
array=(
   '51.0/16501::Baylor'
   '522.0/16501::Oklahoma'
   '521.0/16501::Oklahoma+St.'
   '328.0/16501::Kansas'
   '703.0/16501::Texas'
   '768.0/16501::West+Virginia'
   '700.0/16501::Texas+Tech'
   '311.0/16501::Iowa+St.'
   '698.0/16501::TCU'
   '327.0/16501::Kansas+St.'
   '140.0/16501::Cincinnati'
   '288.0/16501::Houston'
   '128.0/16501::UCF'
   '77.0/16501::BYU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
