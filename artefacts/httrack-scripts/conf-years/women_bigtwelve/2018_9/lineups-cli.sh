#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=women_bigtwelve
array=(
   '51.0/14320::Baylor'
   '311.0/14320::Iowa+St.'
   '522.0/14320::Oklahoma'
   '700.0/14320::Texas+Tech'
   '768.0/14320::West+Virginia'
   '703.0/14320::Texas'
   '698.0/14320::TCU'
   '521.0/14320::Oklahoma+St.'
   '328.0/14320::Kansas'
   '327.0/14320::Kansas+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
