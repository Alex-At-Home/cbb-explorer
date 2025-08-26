#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=american
array=(
   '663.0/15061::SMU'
   '164.0/15061::UConn'
   '140.0/15061::Cincinnati'
   '288.0/15061::Houston'
   '404.0/15061::Memphis'
   '782.0/15061::Wichita+St.'
   '128.0/15061::UCF'
   '718.0/15061::Tulane'
   '690.0/15061::Temple'
   '196.0/15061::East+Carolina'
   '719.0/15061::Tulsa'
   '651.0/15061::South+Fla.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
