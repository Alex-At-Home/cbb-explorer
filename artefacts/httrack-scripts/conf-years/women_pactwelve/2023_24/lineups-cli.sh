#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=women_pactwelve
array=(
   '674.0/16500::Stanford'
   '110.0/16500::UCLA'
   '528.0/16500::Oregon+St.'
   '529.0/16500::Oregon'
   '29.0/16500::Arizona'
   '157.0/16500::Colorado'
   '657.0/16500::Southern+California'
   '754.0/16500::Washington+St.'
   '732.0/16500::Utah'
   '756.0/16500::Washington'
   '28.0/16500::Arizona+St.'
   '107.0/16500::California'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"