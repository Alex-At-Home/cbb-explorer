#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=women_pactwelve
array=(
   '674.0/15500::Stanford'
   '110.0/15500::UCLA'
   '528.0/15500::Oregon+St.'
   '529.0/15500::Oregon'
   '29.0/15500::Arizona'
   '157.0/15500::Colorado'
   '657.0/15500::Southern+California'
   '754.0/15500::Washington+St.'
   '732.0/15500::Utah'
   '756.0/15500::Washington'
   '28.0/15500::Arizona+St.'
   '107.0/15500::California'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
