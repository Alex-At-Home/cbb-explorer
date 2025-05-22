#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=wcc
array=(
   '260.0/15881::Gonzaga'
   '541.0/15881::Pepperdine'
   '77.0/15881::BYU'
   '629.0/15881::San+Francisco'
   '551.0/15881::Portland'
   '370.0/15881::LMU+%28CA%29'
   '610.0/15881::Saint+Mary%27s+%28CA%29'
   '534.0/15881::Pacific'
   '631.0/15881::Santa+Clara'
   '627.0/15881::San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
