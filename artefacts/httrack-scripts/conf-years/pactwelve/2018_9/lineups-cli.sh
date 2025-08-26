#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=pactwelve
array=(
   '110.0/14300::UCLA'
   '28.0/14300::Arizona+St.'
   '657.0/14300::Southern+California'
   '732.0/14300::Utah'
   '754.0/14300::Washington+St.'
   '157.0/14300::Colorado'
   '528.0/14300::Oregon+St.'
   '674.0/14300::Stanford'
   '29.0/14300::Arizona'
   '529.0/14300::Oregon'
   '756.0/14300::Washington'
   '107.0/14300::California'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
