#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=women_american
array=(
   '718.0/16720::Tulane'
   '651.0/16720::South+Fla.'
   '782.0/16720::Wichita+St.'
   '690.0/16720::Temple'
   '404.0/16720::Memphis'
   '196.0/16720::East+Carolina'
   '719.0/16720::Tulsa'
   #(missing some schools taken from C-USA)
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"