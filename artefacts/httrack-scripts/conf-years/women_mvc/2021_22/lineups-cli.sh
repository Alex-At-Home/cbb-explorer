#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=mvc
array=(
   '189.0/15866::Drake'
   '669.0/15866::Missouri+St.'
   '299.0/15866::Illinois+St.'
   '371.0/15866::Loyola+Chicago'
   '72.0/15866::Bradley'
   '659.0/15866::Southern+Ill.'
   '504.0/15866::UNI'
   '735.0/15866::Valparaiso'
   '305.0/15866::Indiana+St.'
   '219.0/15866::Evansville'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
