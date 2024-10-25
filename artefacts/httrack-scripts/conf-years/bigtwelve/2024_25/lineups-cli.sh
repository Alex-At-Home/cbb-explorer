#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigtwelve
array=(
   '51.0/16700::Baylor'
   '521.0/16700::Oklahoma+St.'
   '328.0/16700::Kansas'
   '768.0/16700::West+Virginia'
   '700.0/16700::Texas+Tech'
   '311.0/16700::Iowa+St.'
   '698.0/16700::TCU'
   '327.0/16700::Kansas+St.'
   '140.0/16700::Cincinnati'
   '288.0/16700::Houston'
   '128.0/16700::UCF'
   '77.0/16700::BYU'
   '29.0/16700::Arizona'
   '157.0/16700::Colorado'
   '28.0/16700::Arizona+St.'
   '732.0/16700::Utah'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
