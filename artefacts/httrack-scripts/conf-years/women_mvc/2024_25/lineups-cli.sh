#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=mvc
array=(
   '189.0/16720::Drake'
   '669.0/16720::Missouri+St.'
   '299.0/16720::Illinois+St.'
   '72.0/16720::Bradley'
   '659.0/16720::Southern+Ill.'
   '504.0/16720::UNI'
   '735.0/16720::Valparaiso'
   '305.0/16720::Indiana+St.'
   '219.0/16720::Evansville'
   '14927.0/16720::Belmont'
   '454.0/16720::Murray+St.'
   '302.0/16720::UIC'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"