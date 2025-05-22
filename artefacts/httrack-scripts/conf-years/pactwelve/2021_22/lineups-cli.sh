#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=pactwelve
array=(
   '29.0/15881::Arizona'
   '657.0/15881::Southern+California'
   '110.0/15881::UCLA'
   '529.0/15881::Oregon'
   '157.0/15881::Colorado'
   '28.0/15881::Arizona+St.'
   '528.0/15881::Oregon+St.'
   '674.0/15881::Stanford'
   '754.0/15881::Washington+St.'
   '732.0/15881::Utah'
   '107.0/15881::California'
   '756.0/15881::Washington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
