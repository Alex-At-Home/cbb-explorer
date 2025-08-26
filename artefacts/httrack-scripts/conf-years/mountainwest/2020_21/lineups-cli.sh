#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=mountainwest
array=(
   '66.0/15480::Boise+St.'
   '811.0/15480::Wyoming'
   '156.0/15480::Colorado+St.'
   '731.0/15480::Utah+St.'
   '465.0/15480::UNLV'
   '466.0/15480::Nevada'
   '96.0/15480::Fresno+St.'
   '626.0/15480::San+Diego+St.'
   '630.0/15480::San+Jose+St.'
   '473.0/15480::New+Mexico'
   '721.0/15480::Air+Force'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
