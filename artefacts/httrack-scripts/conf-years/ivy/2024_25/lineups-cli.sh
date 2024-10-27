#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=ivy
array=(
   '813.0/16700::Yale'
   '80.0/16700::Brown'
   '158.0/16700::Columbia'
   '167.0/16700::Cornell'
   '275.0/16700::Harvard'
   '172.0/16700::Dartmouth'
   '540.0/16700::Penn'
   '554.0/16700::Princeton'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"