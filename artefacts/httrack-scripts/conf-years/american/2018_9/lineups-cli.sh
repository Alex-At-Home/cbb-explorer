#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=american
array=(
   '404.0/14300::Memphis'
   '288.0/14300::Houston'
   '690.0/14300::Temple'
   '164.0/14300::UConn'
   '651.0/14300::South+Fla.'
   '128.0/14300::UCF'
   '663.0/14300::SMU'
   '719.0/14300::Tulsa'
   '140.0/14300::Cincinnati'
   '782.0/14300::Wichita+St.'
   '196.0/14300::East+Carolina'
   '718.0/14300::Tulane'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
