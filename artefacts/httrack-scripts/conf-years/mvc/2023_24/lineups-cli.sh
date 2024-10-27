#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=mvc
array=(
   '189.0/16501::Drake'
   '669.0/16501::Missouri+St.'
   '299.0/16501::Illinois+St.'
   '72.0/16501::Bradley'
   '659.0/16501::Southern+Ill.'
   '504.0/16501::UNI'
   '735.0/16501::Valparaiso'
   '305.0/16501::Indiana+St.'
   '219.0/16501::Evansville'
   '14927.0/16501::Belmont'
   '454.0/16501::Murray+St.'
   '302.0/16501::UIC'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"