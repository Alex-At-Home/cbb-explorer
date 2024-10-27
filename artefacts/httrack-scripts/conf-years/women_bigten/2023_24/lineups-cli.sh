#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=women_bigten
array=(
   '392.0/16500::Maryland'
   '312.0/16500::Iowa'
   '518.0/16500::Ohio+St.'
   '418.0/16500::Michigan'
   '416.0/16500::Michigan+St.'
   '587.0/16500::Rutgers'
   '306.0/16500::Indiana'
   '539.0/16500::Penn+St.'
   '463.0/16500::Nebraska'
   '428.0/16500::Minnesota'
   '509.0/16500::Northwestern'
   '559.0/16500::Purdue'
   '796.0/16500::Wisconsin'
   '301.0/16500::Illinois'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"