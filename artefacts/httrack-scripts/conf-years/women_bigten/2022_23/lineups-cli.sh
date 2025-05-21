#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=women_bigten
array=(
   '392.0/16061::Maryland'
   '312.0/16061::Iowa'
   '518.0/16061::Ohio+St.'
   '418.0/16061::Michigan'
   '416.0/16061::Michigan+St.'
   '587.0/16061::Rutgers'
   '306.0/16061::Indiana'
   '539.0/16061::Penn+St.'
   '463.0/16061::Nebraska'
   '428.0/16061::Minnesota'
   '509.0/16061::Northwestern'
   '559.0/16061::Purdue'
   '796.0/16061::Wisconsin'
   '301.0/16061::Illinois'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
