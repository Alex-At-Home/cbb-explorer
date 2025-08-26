#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=bigtwelve
array=(
   '311.0/14300::Iowa+St.'
   '328.0/14300::Kansas'
   '698.0/14300::TCU'
   '768.0/14300::West+Virginia'
   '700.0/14300::Texas+Tech'
   '51.0/14300::Baylor'
   '522.0/14300::Oklahoma'
   '703.0/14300::Texas'
   '521.0/14300::Oklahoma+St.'
   '327.0/14300::Kansas+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
