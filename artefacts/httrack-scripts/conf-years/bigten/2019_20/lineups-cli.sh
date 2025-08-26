#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=bigten
array=(
   '312.0/15061::Iowa'
   '416.0/15061::Michigan+St.'
   '539.0/15061::Penn+St.'
   '418.0/15061::Michigan'
   '301.0/15061::Illinois'
   '518.0/15061::Ohio+St.'
   '392.0/15061::Maryland'
   '306.0/15061::Indiana'
   '428.0/15061::Minnesota'
   '587.0/15061::Rutgers'
   '463.0/15061::Nebraska'
   '559.0/15061::Purdue'
   '796.0/15061::Wisconsin'
   '509.0/15061::Northwestern'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
