#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=mountainwest
array=(
   '66.0/16500::Boise+St.'
   '811.0/16500::Wyoming'
   '156.0/16500::Colorado+St.'
   '731.0/16500::Utah+St.'
   '465.0/16500::UNLV'
   '466.0/16500::Nevada'
   '96.0/16500::Fresno+St.'
   '626.0/16500::San+Diego+St.'
   '630.0/16500::San+Jose+St.'
   '473.0/16500::New+Mexico'
   '721.0/16500::Air+Force'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"