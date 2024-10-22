#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=bigten
array=(
   '312.0/16501::Iowa'
   '301.0/16501::Illinois'
   '418.0/16501::Michigan'
   '428.0/16501::Minnesota'
   '509.0/16501::Northwestern'
   '539.0/16501::Penn+St.'
   '518.0/16501::Ohio+St.'
   '416.0/16501::Michigan+St.'
   '463.0/16501::Nebraska'
   '587.0/16501::Rutgers'
   '392.0/16501::Maryland'
   '796.0/16501::Wisconsin'
   '306.0/16501::Indiana'
   '559.0/16501::Purdue'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"