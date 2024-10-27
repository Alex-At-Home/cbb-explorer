#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=horizon
array=(
   '810.0/16501::Wright+St.'
   '308.0/16501::Purdue+Fort+Wayne'
   '817.0/16501::Youngstown+St.'
   '797.0/16501::Milwaukee'
   '184.0/16501::Detroit+Mercy'
   '579.0/16501::Robert+Morris'
   '514.0/16501::Oakland'
   '794.0/16501::Green+Bay'
   '505.0/16501::Northern+Ky.'
   '148.0/16501::Cleveland+St.'
   '2699.0/16501::IUPUI'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"