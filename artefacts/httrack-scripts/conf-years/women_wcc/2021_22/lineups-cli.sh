#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=wcc
array=(
   '260.0/15866::Gonzaga'
   '541.0/15866::Pepperdine'
   '77.0/15866::BYU'
   '629.0/15866::San+Francisco'
   '551.0/15866::Portland'
   '370.0/15866::LMU+%28CA%29'
   '610.0/15866::Saint+Mary%27s+%28CA%29'
   '534.0/15866::Pacific'
   '631.0/15866::Santa+Clara'
   '627.0/15866::San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
