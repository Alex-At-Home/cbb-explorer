#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=american
array=(
   '782.0/16700::Wichita+St.'
   '404.0/16700::Memphis'
   '196.0/16700::East+Carolina'
   '651.0/16700::South+Fla.'
   '719.0/16700::Tulsa'
   '690.0/16700::Temple'
   '718.0/16700::Tulane'
   '458.0/16700::Charlotte'
   '229.0/16700::Fla.+Atlantic'
   '497.0/16700::North+Texas'
   '574.0/16700::Rice'
   '9.0/16700::UAB'
   '706.0/16700::UTSA'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"