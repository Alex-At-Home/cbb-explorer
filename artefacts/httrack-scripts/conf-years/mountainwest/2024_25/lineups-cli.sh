#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=mountainwest
array=(
   '66.0/16700::Boise+St.'
   '811.0/16700::Wyoming'
   '156.0/16700::Colorado+St.'
   '731.0/16700::Utah+St.'
   '465.0/16700::UNLV'
   '466.0/16700::Nevada'
   '96.0/16700::Fresno+St.'
   '626.0/16700::San+Diego+St.'
   '630.0/16700::San+Jose+St.'
   '473.0/16700::New+Mexico'
   '721.0/16700::Air+Force'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"