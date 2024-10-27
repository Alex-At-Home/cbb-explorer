#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=meac
array=(
   '446.0/16501::Morgan+St.'
   '485.0/16501::Norfolk+St.'
   '290.0/16501::Howard'
   '489.0/16501::N.C.+Central'
   '178.0/16501::Delaware+St.'
   '165.0/16501::Coppin+St.'
   '647.0/16501::South+Carolina+St.'
   '393.0/16501::UMES'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"