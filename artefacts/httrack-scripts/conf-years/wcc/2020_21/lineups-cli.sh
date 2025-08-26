#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=wcc
array=(
   '260.0/15480::Gonzaga'
   '541.0/15480::Pepperdine'
   '77.0/15480::BYU'
   '629.0/15480::San+Francisco'
   '551.0/15480::Portland'
   '370.0/15480::LMU+%28CA%29'
   '610.0/15480::Saint+Mary%27s+%28CA%29'
   '534.0/15480::Pacific'
   '631.0/15480::Santa+Clara'
   '627.0/15480::San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
