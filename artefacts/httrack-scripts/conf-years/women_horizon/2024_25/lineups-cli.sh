#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=horizon
array=(
   '810.0/16720::Wright+St.'
   '308.0/16720::Purdue+Fort+Wayne'
   '817.0/16720::Youngstown+St.'
   '797.0/16720::Milwaukee'
   '184.0/16720::Detroit+Mercy'
   '579.0/16720::Robert+Morris'
   '514.0/16720::Oakland'
   '794.0/16720::Green+Bay'
   '505.0/16720::Northern+Ky.'
   '148.0/16720::Cleveland+St.'
   '2699.0/16720::IU+Indy'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"