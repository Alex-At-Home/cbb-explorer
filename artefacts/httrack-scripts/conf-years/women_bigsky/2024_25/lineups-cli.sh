#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigsky
array=(
   '758.0/16720::Weber+St.'
   '440.0/16720::Montana+St.'
   '207.0/16720::Eastern+Wash.'
   '102.0/16720::Sacramento+St.'
   '294.0/16720::Idaho+St.'
   '550.0/16720::Portland+St.'
   '441.0/16720::Montana'
   '502.0/16720::Northern+Colo.'
   '501.0/16720::Northern+Ariz.'
   '295.0/16720::Idaho'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"