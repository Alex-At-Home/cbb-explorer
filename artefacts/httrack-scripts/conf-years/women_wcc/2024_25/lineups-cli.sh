#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=wcc
array=(
   '260.0/16720::Gonzaga'
   '541.0/16720::Pepperdine'
   '629.0/16720::San+Francisco'
   '551.0/16720::Portland'
   '370.0/16720::LMU+%28CA%29'
   '610.0/16720::Saint+Mary%27s+%28CA%29'
   '534.0/16720::Pacific'
   '631.0/16720::Santa+Clara'
   '627.0/16720::San+Diego'
   '528.0/16720::Oregon+St.'
   '754.0/16720::Washington+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"