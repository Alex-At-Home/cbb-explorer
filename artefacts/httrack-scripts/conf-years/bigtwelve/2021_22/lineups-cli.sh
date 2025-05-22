#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=bigtwelve
array=(
   '51.0/15881::Baylor'
   '522.0/15881::Oklahoma'
   '521.0/15881::Oklahoma+St.'
   '328.0/15881::Kansas'
   '703.0/15881::Texas'
   '768.0/15881::West+Virginia'
   '700.0/15881::Texas+Tech'
   '311.0/15881::Iowa+St.'
   '698.0/15881::TCU'
   '327.0/15881::Kansas+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
