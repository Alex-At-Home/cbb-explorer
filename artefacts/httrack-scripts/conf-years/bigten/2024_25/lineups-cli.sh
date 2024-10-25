#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigten
array=(
   '312.0/16700::Iowa'
   '301.0/16700::Illinois'
   '418.0/16700::Michigan'
   '428.0/16700::Minnesota'
   '509.0/16700::Northwestern'
   '539.0/16700::Penn+St.'
   '518.0/16700::Ohio+St.'
   '416.0/16700::Michigan+St.'
   '463.0/16700::Nebraska'
   '587.0/16700::Rutgers'
   '392.0/16700::Maryland'
   '796.0/16700::Wisconsin'
   '306.0/16700::Indiana'
   '559.0/16700::Purdue'
   '657.0/16700::Southern+California'
   '110.0/16700::UCLA'
   '529.0/16700::Oregon'
   '756.0/16700::Washington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
