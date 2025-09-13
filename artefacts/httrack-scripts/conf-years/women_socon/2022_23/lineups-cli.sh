#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=socon
array=(
   '141.0/16061::The+Citadel'
   '244.0/16061::Furman'
   '769.0/16061::Western+Caro.'
   '741.0/16061::VMI'
   '625.0/16061::Samford'
   '406.0/16061::Mercer'
   '2915.0/16061::Wofford'
   '693.0/16061::Chattanooga'
   '459.0/16061::UNC+Greensboro'
   '198.0/16061::ETSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
