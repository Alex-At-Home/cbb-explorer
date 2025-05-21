#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=women_bigtwelve
array=(
   '51.0/16061::Baylor'
   '311.0/16061::Iowa+St.'
   '522.0/16061::Oklahoma'
   '768.0/16061::West+Virginia'
   '700.0/16061::Texas+Tech'
   '521.0/16061::Oklahoma+St.'
   '328.0/16061::Kansas'
   '703.0/16061::Texas'
   '698.0/16061::TCU'
   '327.0/16061::Kansas+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
