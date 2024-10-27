#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=meac
array=(
   '446.0/16700::Morgan+St.'
   '485.0/16700::Norfolk+St.'
   '290.0/16700::Howard'
   '489.0/16700::N.C.+Central'
   '178.0/16700::Delaware+St.'
   '165.0/16700::Coppin+St.'
   '647.0/16700::South+Carolina+St.'
   '393.0/16700::UMES'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"