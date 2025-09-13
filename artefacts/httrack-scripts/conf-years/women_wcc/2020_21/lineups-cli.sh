#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=wcc
array=(
   '260.0/15500::Gonzaga'
   '541.0/15500::Pepperdine'
   '77.0/15500::BYU'
   '629.0/15500::San+Francisco'
   '551.0/15500::Portland'
   '370.0/15500::LMU+%28CA%29'
   '610.0/15500::Saint+Mary%27s+%28CA%29'
   '534.0/15500::Pacific'
   '631.0/15500::Santa+Clara'
   '627.0/15500::San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
