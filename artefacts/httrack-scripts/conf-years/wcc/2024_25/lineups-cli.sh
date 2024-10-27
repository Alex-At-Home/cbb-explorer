#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=wcc
array=(
   '260.0/16700::Gonzaga'
   '541.0/16700::Pepperdine'
   '629.0/16700::San+Francisco'
   '551.0/16700::Portland'
   '370.0/16700::LMU+%28CA%29'
   '610.0/16700::Saint+Mary%27s+%28CA%29'
   '534.0/16700::Pacific'
   '631.0/16700::Santa+Clara'
   '627.0/16700::San+Diego'
   '528.0/16700::Oregon+St.'
   '754.0/16700::Washington+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"