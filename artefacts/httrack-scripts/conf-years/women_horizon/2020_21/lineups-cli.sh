#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=horizon
array=(
   '810.0/15500::Wright+St.'
   '308.0/15500::Purdue+Fort+Wayne'
   '817.0/15500::Youngstown+St.'
   '797.0/15500::Milwaukee'
   '184.0/15500::Detroit+Mercy'
   '579.0/15500::Robert+Morris'
   '514.0/15500::Oakland'
   '302.0/15500::UIC'
   '794.0/15500::Green+Bay'
   '505.0/15500::Northern+Ky.'
   '148.0/15500::Cleveland+St.'
   '2699.0/15500::IUPUI'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
