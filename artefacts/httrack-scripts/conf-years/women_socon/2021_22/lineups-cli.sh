#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=socon
array=(
   '141.0/15866::The+Citadel'
   '244.0/15866::Furman'
   '769.0/15866::Western+Caro.'
   '741.0/15866::VMI'
   '625.0/15866::Samford'
   '406.0/15866::Mercer'
   '2915.0/15866::Wofford'
   '693.0/15866::Chattanooga'
   '459.0/15866::UNC+Greensboro'
   '198.0/15866::ETSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
