#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=women_pactwelve
array=(
   '674.0/15866::Stanford'
   '110.0/15866::UCLA'
   '528.0/15866::Oregon+St.'
   '529.0/15866::Oregon'
   '29.0/15866::Arizona'
   '157.0/15866::Colorado'
   '657.0/15866::Southern+California'
   '754.0/15866::Washington+St.'
   '732.0/15866::Utah'
   '756.0/15866::Washington'
   '28.0/15866::Arizona+St.'
   '107.0/15866::California'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
