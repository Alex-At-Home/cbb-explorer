#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=horizon
array=(
   '810.0/16500::Wright+St.'
   '308.0/16500::Purdue+Fort+Wayne'
   '817.0/16500::Youngstown+St.'
   '797.0/16500::Milwaukee'
   '184.0/16500::Detroit+Mercy'
   '579.0/16500::Robert+Morris'
   '514.0/16500::Oakland'
   '794.0/16500::Green+Bay'
   '505.0/16500::Northern+Ky.'
   '148.0/16500::Cleveland+St.'
   '2699.0/16500::IUPUI'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"