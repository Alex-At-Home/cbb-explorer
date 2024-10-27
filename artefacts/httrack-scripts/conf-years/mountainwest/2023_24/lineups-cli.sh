#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=mountainwest
array=(
   '66.0/16501::Boise+St.'
   '811.0/16501::Wyoming'
   '156.0/16501::Colorado+St.'
   '731.0/16501::Utah+St.'
   '465.0/16501::UNLV'
   '466.0/16501::Nevada'
   '96.0/16501::Fresno+St.'
   '626.0/16501::San+Diego+St.'
   '630.0/16501::San+Jose+St.'
   '473.0/16501::New+Mexico'
   '721.0/16501::Air+Force'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"