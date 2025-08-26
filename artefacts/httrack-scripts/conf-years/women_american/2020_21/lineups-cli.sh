#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=women_american
array=(
   '718.0/15500::Tulane'
   '288.0/15500::Houston'
   '651.0/15500::South+Fla.'
   '782.0/15500::Wichita+St.'
   '690.0/15500::Temple'
   '140.0/15500::Cincinnati'
   '404.0/15500::Memphis'
   '196.0/15500::East+Carolina'
   '128.0/15500::UCF'
   '719.0/15500::Tulsa'
   '663.0/15500::SMU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
