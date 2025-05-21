#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=ivy
array=(
   '813.0/16060::Yale'
   '80.0/16060::Brown'
   '158.0/16060::Columbia'
   '167.0/16060::Cornell'
   '275.0/16060::Harvard'
   '172.0/16060::Dartmouth'
   '540.0/16060::Penn'
   '554.0/16060::Princeton'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
