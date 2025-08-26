#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=horizon
array=(
   '810.0/15480::Wright+St.'
   '308.0/15480::Purdue+Fort+Wayne'
   '817.0/15480::Youngstown+St.'
   '797.0/15480::Milwaukee'
   '184.0/15480::Detroit+Mercy'
   '579.0/15480::Robert+Morris'
   '514.0/15480::Oakland'
   '302.0/15480::UIC'
   '794.0/15480::Green+Bay'
   '505.0/15480::Northern+Ky.'
   '148.0/15480::Cleveland+St.'
   '2699.0/15480::IUPUI'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
