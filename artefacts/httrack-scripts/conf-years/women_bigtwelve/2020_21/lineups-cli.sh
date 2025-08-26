#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=women_bigtwelve
array=(
   '51.0/15500::Baylor'
   '311.0/15500::Iowa+St.'
   '522.0/15500::Oklahoma'
   '768.0/15500::West+Virginia'
   '700.0/15500::Texas+Tech'
   '521.0/15500::Oklahoma+St.'
   '328.0/15500::Kansas'
   '703.0/15500::Texas'
   '698.0/15500::TCU'
   '327.0/15500::Kansas+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
