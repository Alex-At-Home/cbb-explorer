#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=socon
array=(
   '141.0/15500::The+Citadel'
   '244.0/15500::Furman'
   '769.0/15500::Western+Caro.'
   '741.0/15500::VMI'
   '625.0/15500::Samford'
   '406.0/15500::Mercer'
   '2915.0/15500::Wofford'
   '693.0/15500::Chattanooga'
   '459.0/15500::UNC+Greensboro'
   '198.0/15500::ETSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
