#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=women_bigtwelve
array=(
   '51.0/16720::Baylor'
   '311.0/16720::Iowa+St.'
   '768.0/16720::West+Virginia'
   '700.0/16720::Texas+Tech'
   '521.0/16720::Oklahoma+St.'
   '328.0/16720::Kansas'
   '698.0/16720::TCU'
   '327.0/16720::Kansas+St.'
   '140.0/16720::Cincinnati'
   '288.0/16720::Houston'
   '128.0/16720::UCF'
   '77.0/16720::BYU'
   '29.0/16720::Arizona'
   '157.0/16720::Colorado'
   '732.0/16720::Utah'
   '28.0/16720::Arizona+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"