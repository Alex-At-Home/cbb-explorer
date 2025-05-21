#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=bigten
array=(
   '312.0/16060::Iowa'
   '301.0/16060::Illinois'
   '418.0/16060::Michigan'
   '428.0/16060::Minnesota'
   '509.0/16060::Northwestern'
   '539.0/16060::Penn+St.'
   '518.0/16060::Ohio+St.'
   '416.0/16060::Michigan+St.'
   '463.0/16060::Nebraska'
   '587.0/16060::Rutgers'
   '392.0/16060::Maryland'
   '796.0/16060::Wisconsin'
   '306.0/16060::Indiana'
   '559.0/16060::Purdue'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
