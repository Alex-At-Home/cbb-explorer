#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=american
array=(
   '663.0/15881::SMU'
   '782.0/15881::Wichita+St.'
   '288.0/15881::Houston'
   '404.0/15881::Memphis'
   '140.0/15881::Cincinnati'
   '196.0/15881::East+Carolina'
   '651.0/15881::South+Fla.'
   '719.0/15881::Tulsa'
   '690.0/15881::Temple'
   '128.0/15881::UCF'
   '718.0/15881::Tulane'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
