#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=women_american
array=(
   '718.0/16500::Tulane'
   '651.0/16500::South+Fla.'
   '782.0/16500::Wichita+St.'
   '690.0/16500::Temple'
   '404.0/16500::Memphis'
   '196.0/16500::East+Carolina'
   '719.0/16500::Tulsa'
   '663.0/16500::SMU'
   #(missing some schools taken from C-USA)
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"