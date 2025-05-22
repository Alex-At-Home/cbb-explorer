#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=mountainwest
array=(
   '66.0/15881::Boise+St.'
   '811.0/15881::Wyoming'
   '156.0/15881::Colorado+St.'
   '731.0/15881::Utah+St.'
   '465.0/15881::UNLV'
   '466.0/15881::Nevada'
   '96.0/15881::Fresno+St.'
   '626.0/15881::San+Diego+St.'
   '630.0/15881::San+Jose+St.'
   '473.0/15881::New+Mexico'
   '721.0/15881::Air+Force'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
