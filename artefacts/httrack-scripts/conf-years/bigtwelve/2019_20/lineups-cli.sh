#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=bigtwelve
array=(
   '328.0/15061::Kansas'
   '311.0/15061::Iowa+St.'
   '700.0/15061::Texas+Tech'
   '51.0/15061::Baylor'
   '768.0/15061::West+Virginia'
   '522.0/15061::Oklahoma'
   '521.0/15061::Oklahoma+St.'
   '698.0/15061::TCU'
   '327.0/15061::Kansas+St.'
   '703.0/15061::Texas'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
