#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=ivy
array=(
   '813.0/15866::Yale'
   '80.0/15866::Brown'
   '158.0/15866::Columbia'
   '167.0/15866::Cornell'
   '275.0/15866::Harvard'
   '172.0/15866::Dartmouth'
   '540.0/15866::Penn'
   '554.0/15866::Princeton'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
