#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=women_bigten
array=(
   '392.0/15002::Maryland'
   '312.0/15002::Iowa'
   '306.0/15002::Indiana'
   '518.0/15002::Ohio+St.'
   '509.0/15002::Northwestern'
   '418.0/15002::Michigan'
   '428.0/15002::Minnesota'
   '463.0/15002::Nebraska'
   '416.0/15002::Michigan+St.'
   '539.0/15002::Penn+St.'
   '587.0/15002::Rutgers'
   '796.0/15002::Wisconsin'
   '559.0/15002::Purdue'
   '301.0/15002::Illinois'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
