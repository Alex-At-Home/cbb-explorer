#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=ivy
array=(
   '813.0/16501::Yale'
   '80.0/16501::Brown'
   '158.0/16501::Columbia'
   '167.0/16501::Cornell'
   '275.0/16501::Harvard'
   '172.0/16501::Dartmouth'
   '540.0/16501::Penn'
   '554.0/16501::Princeton'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"