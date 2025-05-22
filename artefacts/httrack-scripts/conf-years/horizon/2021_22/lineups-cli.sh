#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=horizon
array=(
   '810.0/15881::Wright+St.'
   '308.0/15881::Purdue+Fort+Wayne'
   '817.0/15881::Youngstown+St.'
   '797.0/15881::Milwaukee'
   '184.0/15881::Detroit+Mercy'
   '579.0/15881::Robert+Morris'
   '514.0/15881::Oakland'
   '302.0/15881::UIC'
   '794.0/15881::Green+Bay'
   '505.0/15881::Northern+Ky.'
   '148.0/15881::Cleveland+St.'
   '2699.0/15881::IUPUI'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
