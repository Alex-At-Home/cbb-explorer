#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=women_american
array=(
   '164.0/14320::UConn'
   '288.0/14320::Houston'
   '140.0/14320::Cincinnati'
   '690.0/14320::Temple'
   '651.0/14320::South+Fla.'
   '718.0/14320::Tulane'
   '196.0/14320::East+Carolina'
   '128.0/14320::UCF'
   '719.0/14320::Tulsa'
   '404.0/14320::Memphis'
   '782.0/14320::Wichita+St.'
   '663.0/14320::SMU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
