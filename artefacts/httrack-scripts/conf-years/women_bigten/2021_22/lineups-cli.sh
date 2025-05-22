#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=women_bigten
array=(
   '392.0/15866::Maryland'
   '312.0/15866::Iowa'
   '518.0/15866::Ohio+St.'
   '418.0/15866::Michigan'
   '416.0/15866::Michigan+St.'
   '587.0/15866::Rutgers'
   '306.0/15866::Indiana'
   '539.0/15866::Penn+St.'
   '463.0/15866::Nebraska'
   '428.0/15866::Minnesota'
   '509.0/15866::Northwestern'
   '559.0/15866::Purdue'
   '796.0/15866::Wisconsin'
   '301.0/15866::Illinois'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
