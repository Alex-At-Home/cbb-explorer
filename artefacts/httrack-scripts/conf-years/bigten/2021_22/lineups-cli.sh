#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=bigten
array=(
   '312.0/15881::Iowa'
   '301.0/15881::Illinois'
   '418.0/15881::Michigan'
   '428.0/15881::Minnesota'
   '509.0/15881::Northwestern'
   '539.0/15881::Penn+St.'
   '518.0/15881::Ohio+St.'
   '416.0/15881::Michigan+St.'
   '463.0/15881::Nebraska'
   '587.0/15881::Rutgers'
   '392.0/15881::Maryland'
   '796.0/15881::Wisconsin'
   '306.0/15881::Indiana'
   '559.0/15881::Purdue'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
