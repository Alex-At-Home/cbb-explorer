#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=meac
array=(
   '446.0/15500::Morgan+St.'
   '488.0/15500::N.C.+A%26T'
   '485.0/15500::Norfolk+St.'
   '290.0/15500::Howard'
   '489.0/15500::N.C.+Central'
   '178.0/15500::Delaware+St.'
   '165.0/15500::Coppin+St.'
   '228.0/15500::Florida+A%26M'
   '647.0/15500::South+Carolina+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
