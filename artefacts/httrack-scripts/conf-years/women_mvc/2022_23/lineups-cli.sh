#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=mvc
array=(
   '189.0/16061::Drake'
   '669.0/16061::Missouri+St.'
   '299.0/16061::Illinois+St.'
   '72.0/16061::Bradley'
   '659.0/16061::Southern+Ill.'
   '504.0/16061::UNI'
   '735.0/16061::Valparaiso'
   '305.0/16061::Indiana+St.'
   '219.0/16061::Evansville'
   '14927.0/16061::Belmont'
   '454.0/16061::Murray+St.'
   '302.0/16061::UIC'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
