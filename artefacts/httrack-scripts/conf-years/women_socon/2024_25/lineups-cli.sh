#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=socon
array=(
   '141.0/16720::The+Citadel'
   '244.0/16720::Furman'
   '769.0/16720::Western+Caro.'
   '741.0/16720::VMI'
   '625.0/16720::Samford'
   '406.0/16720::Mercer'
   '2915.0/16720::Wofford'
   '693.0/16720::Chattanooga'
   '459.0/16720::UNC+Greensboro'
   '198.0/16720::ETSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"