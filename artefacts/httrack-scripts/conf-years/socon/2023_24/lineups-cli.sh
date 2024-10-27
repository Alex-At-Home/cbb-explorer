#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=socon
array=(
   '141.0/16501::The+Citadel'
   '244.0/16501::Furman'
   '769.0/16501::Western+Caro.'
   '741.0/16501::VMI'
   '625.0/16501::Samford'
   '406.0/16501::Mercer'
   '2915.0/16501::Wofford'
   '693.0/16501::Chattanooga'
   '459.0/16501::UNC+Greensboro'
   '198.0/16501::ETSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"