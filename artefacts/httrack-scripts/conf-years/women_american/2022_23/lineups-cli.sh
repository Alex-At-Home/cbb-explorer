#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=women_american
array=(
   '718.0/16061::Tulane'
   '288.0/16061::Houston'
   '651.0/16061::South+Fla.'
   '782.0/16061::Wichita+St.'
   '690.0/16061::Temple'
   '140.0/16061::Cincinnati'
   '404.0/16061::Memphis'
   '196.0/16061::East+Carolina'
   '128.0/16061::UCF'
   '719.0/16061::Tulsa'
   '663.0/16061::SMU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
