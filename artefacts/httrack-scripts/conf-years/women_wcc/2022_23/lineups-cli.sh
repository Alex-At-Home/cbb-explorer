#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=wcc
array=(
   '260.0/16061::Gonzaga'
   '541.0/16061::Pepperdine'
   '77.0/16061::BYU'
   '629.0/16061::San+Francisco'
   '551.0/16061::Portland'
   '370.0/16061::LMU+%28CA%29'
   '610.0/16061::Saint+Mary%27s+%28CA%29'
   '534.0/16061::Pacific'
   '631.0/16061::Santa+Clara'
   '627.0/16061::San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
