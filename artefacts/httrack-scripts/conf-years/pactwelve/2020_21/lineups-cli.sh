#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=pactwelve
array=(
   '29.0/15480::Arizona'
   '657.0/15480::Southern+California'
   '110.0/15480::UCLA'
   '529.0/15480::Oregon'
   '157.0/15480::Colorado'
   '28.0/15480::Arizona+St.'
   '528.0/15480::Oregon+St.'
   '674.0/15480::Stanford'
   '754.0/15480::Washington+St.'
   '732.0/15480::Utah'
   '107.0/15480::California'
   '756.0/15480::Washington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
