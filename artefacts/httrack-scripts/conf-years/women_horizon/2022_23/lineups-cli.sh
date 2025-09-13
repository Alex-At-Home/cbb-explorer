#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=horizon
array=(
   '810.0/16061::Wright+St.'
   '308.0/16061::Purdue+Fort+Wayne'
   '817.0/16061::Youngstown+St.'
   '797.0/16061::Milwaukee'
   '184.0/16061::Detroit+Mercy'
   '579.0/16061::Robert+Morris'
   '514.0/16061::Oakland'
   '794.0/16061::Green+Bay'
   '505.0/16061::Northern+Ky.'
   '148.0/16061::Cleveland+St.'
   '2699.0/16061::IUPUI'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
