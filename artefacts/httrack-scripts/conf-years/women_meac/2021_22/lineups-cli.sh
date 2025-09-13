#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=meac
array=(
   '446.0/15866::Morgan+St.'
   '485.0/15866::Norfolk+St.'
   '290.0/15866::Howard'
   '489.0/15866::N.C.+Central'
   '178.0/15866::Delaware+St.'
   '165.0/15866::Coppin+St.'
   '647.0/15866::South+Carolina+St.'
   '393.0/15866::UMES'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
