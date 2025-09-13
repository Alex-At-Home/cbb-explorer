#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=socon
array=(
   '141.0/16500::The+Citadel'
   '244.0/16500::Furman'
   '769.0/16500::Western+Caro.'
   '741.0/16500::VMI'
   '625.0/16500::Samford'
   '406.0/16500::Mercer'
   '2915.0/16500::Wofford'
   '693.0/16500::Chattanooga'
   '459.0/16500::UNC+Greensboro'
   '198.0/16500::ETSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"