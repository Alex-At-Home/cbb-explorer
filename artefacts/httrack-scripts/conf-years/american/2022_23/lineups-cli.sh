#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=american
array=(
   '663.0/16060::SMU'
   '782.0/16060::Wichita+St.'
   '288.0/16060::Houston'
   '404.0/16060::Memphis'
   '140.0/16060::Cincinnati'
   '196.0/16060::East+Carolina'
   '651.0/16060::South+Fla.'
   '719.0/16060::Tulsa'
   '690.0/16060::Temple'
   '128.0/16060::UCF'
   '718.0/16060::Tulane'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
