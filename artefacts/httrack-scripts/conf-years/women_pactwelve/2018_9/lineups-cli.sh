#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=women_pactwelve
array=(
   '529.0/14320::Oregon'
   '528.0/14320::Oregon+St.'
   '732.0/14320::Utah'
   '674.0/14320::Stanford'
   '110.0/14320::UCLA'
   '107.0/14320::California'
   '657.0/14320::Southern+California'
   '29.0/14320::Arizona'
   '754.0/14320::Washington+St.'
   '28.0/14320::Arizona+St.'
   '157.0/14320::Colorado'
   '756.0/14320::Washington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
