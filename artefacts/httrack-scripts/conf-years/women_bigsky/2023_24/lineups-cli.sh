#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=bigsky
array=(
   '758.0/16500::Weber+St.'
   '440.0/16500::Montana+St.'
   '207.0/16500::Eastern+Wash.'
   '102.0/16500::Sacramento+St.'
   '294.0/16500::Idaho+St.'
   '550.0/16500::Portland+St.'
   '441.0/16500::Montana'
   '502.0/16500::Northern+Colo.'
   '501.0/16500::Northern+Ariz.'
   '295.0/16500::Idaho'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"