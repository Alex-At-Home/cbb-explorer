#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=women_american
array=(
   '718.0/15866::Tulane'
   '288.0/15866::Houston'
   '651.0/15866::South+Fla.'
   '782.0/15866::Wichita+St.'
   '690.0/15866::Temple'
   '140.0/15866::Cincinnati'
   '404.0/15866::Memphis'
   '196.0/15866::East+Carolina'
   '128.0/15866::UCF'
   '719.0/15866::Tulsa'
   '663.0/15866::SMU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
