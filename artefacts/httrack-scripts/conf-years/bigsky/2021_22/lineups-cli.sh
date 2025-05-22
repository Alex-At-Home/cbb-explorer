#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=bigsky
array=(
   '758.0/15881::Weber+St.'
   '667.0/15881::Southern+Utah'
   '440.0/15881::Montana+St.'
   '207.0/15881::Eastern+Wash.'
   '102.0/15881::Sacramento+St.'
   '294.0/15881::Idaho+St.'
   '550.0/15881::Portland+St.'
   '441.0/15881::Montana'
   '502.0/15881::Northern+Colo.'
   '501.0/15881::Northern+Ariz.'
   '295.0/15881::Idaho'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
