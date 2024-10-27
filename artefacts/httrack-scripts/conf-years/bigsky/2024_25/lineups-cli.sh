#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigsky
array=(
   '758.0/16700::Weber+St.'
   '440.0/16700::Montana+St.'
   '207.0/16700::Eastern+Wash.'
   '102.0/16700::Sacramento+St.'
   '294.0/16700::Idaho+St.'
   '550.0/16700::Portland+St.'
   '441.0/16700::Montana'
   '502.0/16700::Northern+Colo.'
   '501.0/16700::Northern+Ariz.'
   '295.0/16700::Idaho'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"