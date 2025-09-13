#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=wcc
array=(
   '260.0/16500::Gonzaga'
   '541.0/16500::Pepperdine'
   '629.0/16500::San+Francisco'
   '551.0/16500::Portland'
   '370.0/16500::LMU+%28CA%29'
   '610.0/16500::Saint+Mary%27s+%28CA%29'
   '534.0/16500::Pacific'
   '631.0/16500::Santa+Clara'
   '627.0/16500::San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"