#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=mvc
array=(
   '189.0/15881::Drake'
   '669.0/15881::Missouri+St.'
   '299.0/15881::Illinois+St.'
   '371.0/15881::Loyola+Chicago'
   '72.0/15881::Bradley'
   '659.0/15881::Southern+Ill.'
   '504.0/15881::UNI'
   '735.0/15881::Valparaiso'
   '305.0/15881::Indiana+St.'
   '219.0/15881::Evansville'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
