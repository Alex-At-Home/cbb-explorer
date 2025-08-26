#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=women_bigten
array=(
   '392.0/15500::Maryland'
   '312.0/15500::Iowa'
   '518.0/15500::Ohio+St.'
   '418.0/15500::Michigan'
   '416.0/15500::Michigan+St.'
   '587.0/15500::Rutgers'
   '306.0/15500::Indiana'
   '539.0/15500::Penn+St.'
   '463.0/15500::Nebraska'
   '428.0/15500::Minnesota'
   '509.0/15500::Northwestern'
   '559.0/15500::Purdue'
   '796.0/15500::Wisconsin'
   '301.0/15500::Illinois'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
