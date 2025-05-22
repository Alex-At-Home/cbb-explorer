#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=women_bigtwelve
array=(
   '51.0/15866::Baylor'
   '311.0/15866::Iowa+St.'
   '522.0/15866::Oklahoma'
   '768.0/15866::West+Virginia'
   '700.0/15866::Texas+Tech'
   '521.0/15866::Oklahoma+St.'
   '328.0/15866::Kansas'
   '703.0/15866::Texas'
   '698.0/15866::TCU'
   '327.0/15866::Kansas+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
