#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=mvc
array=(
   '189.0/16500::Drake'
   '669.0/16500::Missouri+St.'
   '299.0/16500::Illinois+St.'
   '72.0/16500::Bradley'
   '659.0/16500::Southern+Ill.'
   '504.0/16500::UNI'
   '735.0/16500::Valparaiso'
   '305.0/16500::Indiana+St.'
   '219.0/16500::Evansville'
   '14927.0/16500::Belmont'
   '454.0/16500::Murray+St.'
   '302.0/16500::UIC'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"