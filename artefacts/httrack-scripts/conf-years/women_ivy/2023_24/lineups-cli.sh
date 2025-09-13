#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=ivy
array=(
   '813.0/16500::Yale'
   '80.0/16500::Brown'
   '158.0/16500::Columbia'
   '167.0/16500::Cornell'
   '275.0/16500::Harvard'
   '172.0/16500::Dartmouth'
   '540.0/16500::Penn'
   '554.0/16500::Princeton'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"