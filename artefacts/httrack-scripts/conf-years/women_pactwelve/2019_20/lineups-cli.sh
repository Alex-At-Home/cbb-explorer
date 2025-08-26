#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=women_pactwelve
array=(
   '529.0/15002::Oregon'
   '674.0/15002::Stanford'
   '110.0/15002::UCLA'
   '528.0/15002::Oregon+St.'
   '29.0/15002::Arizona'
   '732.0/15002::Utah'
   '756.0/15002::Washington'
   '107.0/15002::California'
   '28.0/15002::Arizona+St.'
   '754.0/15002::Washington+St.'
   '657.0/15002::Southern+California'
   '157.0/15002::Colorado'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
