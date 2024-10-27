#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=mvc
array=(
   '189.0/16700::Drake'
   '669.0/16700::Missouri+St.'
   '299.0/16700::Illinois+St.'
   '72.0/16700::Bradley'
   '659.0/16700::Southern+Ill.'
   '504.0/16700::UNI'
   '735.0/16700::Valparaiso'
   '305.0/16700::Indiana+St.'
   '219.0/16700::Evansville'
   '14927.0/16700::Belmont'
   '454.0/16700::Murray+St.'
   '302.0/16700::UIC'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"