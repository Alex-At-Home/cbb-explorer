#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=socon
array=(
   '141.0/15881::The+Citadel'
   '244.0/15881::Furman'
   '769.0/15881::Western+Caro.'
   '741.0/15881::VMI'
   '625.0/15881::Samford'
   '406.0/15881::Mercer'
   '2915.0/15881::Wofford'
   '693.0/15881::Chattanooga'
   '459.0/15881::UNC+Greensboro'
   '198.0/15881::ETSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
