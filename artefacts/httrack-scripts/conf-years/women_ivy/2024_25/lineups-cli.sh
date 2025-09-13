#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=ivy
array=(
   '813.0/16720::Yale'
   '80.0/16720::Brown'
   '158.0/16720::Columbia'
   '167.0/16720::Cornell'
   '275.0/16720::Harvard'
   '172.0/16720::Dartmouth'
   '540.0/16720::Penn'
   '554.0/16720::Princeton'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"