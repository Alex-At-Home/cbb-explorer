#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=bigten
array=(
   '312.0/14300::Iowa'
   '416.0/14300::Michigan+St.'
   '559.0/14300::Purdue'
   '301.0/14300::Illinois'
   '463.0/14300::Nebraska'
   '306.0/14300::Indiana'
   '392.0/14300::Maryland'
   '428.0/14300::Minnesota'
   '539.0/14300::Penn+St.'
   '418.0/14300::Michigan'
   '518.0/14300::Ohio+St.'
   '796.0/14300::Wisconsin'
   '587.0/14300::Rutgers'
   '509.0/14300::Northwestern'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
