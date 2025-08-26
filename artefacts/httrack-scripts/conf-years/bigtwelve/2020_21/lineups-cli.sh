#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=bigtwelve
array=(
   '51.0/15480::Baylor'
   '522.0/15480::Oklahoma'
   '521.0/15480::Oklahoma+St.'
   '328.0/15480::Kansas'
   '703.0/15480::Texas'
   '768.0/15480::West+Virginia'
   '700.0/15480::Texas+Tech'
   '311.0/15480::Iowa+St.'
   '698.0/15480::TCU'
   '327.0/15480::Kansas+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
