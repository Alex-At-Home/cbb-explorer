#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=women_bigtwelve
array=(
   '51.0/15002::Baylor'
   '700.0/15002::Texas+Tech'
   '522.0/15002::Oklahoma'
   '311.0/15002::Iowa+St.'
   '698.0/15002::TCU'
   '703.0/15002::Texas'
   '328.0/15002::Kansas'
   '327.0/15002::Kansas+St.'
   '521.0/15002::Oklahoma+St.'
   '768.0/15002::West+Virginia'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
