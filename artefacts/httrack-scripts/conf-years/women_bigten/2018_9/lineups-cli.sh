#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=women_bigten
array=(
   '312.0/14320::Iowa'
   '416.0/14320::Michigan+St.'
   '392.0/14320::Maryland'
   '463.0/14320::Nebraska'
   '418.0/14320::Michigan'
   '428.0/14320::Minnesota'
   '306.0/14320::Indiana'
   '301.0/14320::Illinois'
   '539.0/14320::Penn+St.'
   '509.0/14320::Northwestern'
   '796.0/14320::Wisconsin'
   '559.0/14320::Purdue'
   '587.0/14320::Rutgers'
   '518.0/14320::Ohio+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
