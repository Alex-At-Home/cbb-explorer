#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=ivy
array=(
   '813.0/16061::Yale'
   '80.0/16061::Brown'
   '158.0/16061::Columbia'
   '167.0/16061::Cornell'
   '275.0/16061::Harvard'
   '172.0/16061::Dartmouth'
   '540.0/16061::Penn'
   '554.0/16061::Princeton'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
