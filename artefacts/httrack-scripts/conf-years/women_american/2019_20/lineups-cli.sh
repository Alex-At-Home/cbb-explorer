#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=women_american
array=(
   '164.0/15002::UConn'
   '140.0/15002::Cincinnati'
   '690.0/15002::Temple'
   '404.0/15002::Memphis'
   '128.0/15002::UCF'
   '651.0/15002::South+Fla.'
   '782.0/15002::Wichita+St.'
   '288.0/15002::Houston'
   '663.0/15002::SMU'
   '718.0/15002::Tulane'
   '719.0/15002::Tulsa'
   '196.0/15002::East+Carolina'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
