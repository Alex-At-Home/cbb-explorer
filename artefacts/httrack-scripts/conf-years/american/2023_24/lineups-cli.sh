#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=american
array=(
   '663.0/16501::SMU'
   '782.0/16501::Wichita+St.'
   '404.0/16501::Memphis'
   '196.0/16501::East+Carolina'
   '651.0/16501::South+Fla.'
   '719.0/16501::Tulsa'
   '690.0/16501::Temple'
   '718.0/16501::Tulane'
   '458.0/16501::Charlotte'
   '229.0/16501::Fla.+Atlantic'
   '497.0/16501::North+Texas'
   '574.0/16501::Rice'
   '9.0/16501::UAB'
   '706.0/16501::UTSA'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"