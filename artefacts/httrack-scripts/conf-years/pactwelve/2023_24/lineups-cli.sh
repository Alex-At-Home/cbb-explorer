#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=pactwelve
array=(
   '29.0/16501::Arizona'
   '657.0/16501::Southern+California'
   '110.0/16501::UCLA'
   '529.0/16501::Oregon'
   '157.0/16501::Colorado'
   '28.0/16501::Arizona+St.'
   '528.0/16501::Oregon+St.'
   '674.0/16501::Stanford'
   '754.0/16501::Washington+St.'
   '732.0/16501::Utah'
   '107.0/16501::California'
   '756.0/16501::Washington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
