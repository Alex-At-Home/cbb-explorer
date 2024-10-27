#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=women_bigtwelve
array=(
   '51.0/16500::Baylor'
   '311.0/16500::Iowa+St.'
   '522.0/16500::Oklahoma'
   '768.0/16500::West+Virginia'
   '700.0/16500::Texas+Tech'
   '521.0/16500::Oklahoma+St.'
   '328.0/16500::Kansas'
   '703.0/16500::Texas'
   '698.0/16500::TCU'
   '327.0/16500::Kansas+St.'
   '140.0/16500::Cincinnati'
   '288.0/16500::Houston'
   '128.0/16500::UCF'
   '77.0/16500::BYU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"