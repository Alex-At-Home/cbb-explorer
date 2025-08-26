#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=american
array=(
   '663.0/15480::SMU'
   '782.0/15480::Wichita+St.'
   '288.0/15480::Houston'
   '404.0/15480::Memphis'
   '140.0/15480::Cincinnati'
   '196.0/15480::East+Carolina'
   '651.0/15480::South+Fla.'
   '719.0/15480::Tulsa'
   '690.0/15480::Temple'
   '128.0/15480::UCF'
   '718.0/15480::Tulane'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
