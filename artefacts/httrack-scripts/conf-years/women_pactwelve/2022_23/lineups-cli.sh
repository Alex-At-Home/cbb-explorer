#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=women_pactwelve
array=(
   '674.0/16061::Stanford'
   '110.0/16061::UCLA'
   '528.0/16061::Oregon+St.'
   '529.0/16061::Oregon'
   '29.0/16061::Arizona'
   '157.0/16061::Colorado'
   '657.0/16061::Southern+California'
   '754.0/16061::Washington+St.'
   '732.0/16061::Utah'
   '756.0/16061::Washington'
   '28.0/16061::Arizona+St.'
   '107.0/16061::California'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
