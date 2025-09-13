#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=horizon
array=(
   '810.0/15866::Wright+St.'
   '308.0/15866::Purdue+Fort+Wayne'
   '817.0/15866::Youngstown+St.'
   '797.0/15866::Milwaukee'
   '184.0/15866::Detroit+Mercy'
   '579.0/15866::Robert+Morris'
   '514.0/15866::Oakland'
   '302.0/15866::UIC'
   '794.0/15866::Green+Bay'
   '505.0/15866::Northern+Ky.'
   '148.0/15866::Cleveland+St.'
   '2699.0/15866::IUPUI'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
