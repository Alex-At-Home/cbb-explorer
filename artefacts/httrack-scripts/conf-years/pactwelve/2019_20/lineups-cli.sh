#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=pactwelve
array=(
   '29.0/15061::Arizona'
   '529.0/15061::Oregon'
   '28.0/15061::Arizona+St.'
   '528.0/15061::Oregon+St.'
   '732.0/15061::Utah'
   '157.0/15061::Colorado'
   '756.0/15061::Washington'
   '657.0/15061::Southern+California'
   '754.0/15061::Washington+St.'
   '674.0/15061::Stanford'
   '110.0/15061::UCLA'
   '107.0/15061::California'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
