#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=bigten
array=(
   '312.0/15480::Iowa'
   '301.0/15480::Illinois'
   '418.0/15480::Michigan'
   '428.0/15480::Minnesota'
   '509.0/15480::Northwestern'
   '539.0/15480::Penn+St.'
   '518.0/15480::Ohio+St.'
   '416.0/15480::Michigan+St.'
   '463.0/15480::Nebraska'
   '587.0/15480::Rutgers'
   '392.0/15480::Maryland'
   '796.0/15480::Wisconsin'
   '306.0/15480::Indiana'
   '559.0/15480::Purdue'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
