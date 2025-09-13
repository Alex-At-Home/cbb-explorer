#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=mvc
array=(
   '189.0/15500::Drake'
   '669.0/15500::Missouri+St.'
   '299.0/15500::Illinois+St.'
   '371.0/15500::Loyola+Chicago'
   '72.0/15500::Bradley'
   '659.0/15500::Southern+Ill.'
   '504.0/15500::UNI'
   '735.0/15500::Valparaiso'
   '305.0/15500::Indiana+St.'
   '219.0/15500::Evansville'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
