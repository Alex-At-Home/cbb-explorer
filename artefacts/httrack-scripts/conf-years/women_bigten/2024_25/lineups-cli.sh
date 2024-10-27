#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=women_bigten
array=(
   '392.0/16720::Maryland'
   '312.0/16720::Iowa'
   '518.0/16720::Ohio+St.'
   '418.0/16720::Michigan'
   '416.0/16720::Michigan+St.'
   '587.0/16720::Rutgers'
   '306.0/16720::Indiana'
   '539.0/16720::Penn+St.'
   '463.0/16720::Nebraska'
   '428.0/16720::Minnesota'
   '509.0/16720::Northwestern'
   '559.0/16720::Purdue'
   '796.0/16720::Wisconsin'
   '301.0/16720::Illinois'
   '110.0/16720::UCLA'
   '529.0/16720::Oregon'
   '657.0/16720::Southern+California'
   '756.0/16720::Washington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"