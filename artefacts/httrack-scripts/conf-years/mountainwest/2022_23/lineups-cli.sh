#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=mountainwest
array=(
   '66.0/16060::Boise+St.'
   '811.0/16060::Wyoming'
   '156.0/16060::Colorado+St.'
   '731.0/16060::Utah+St.'
   '465.0/16060::UNLV'
   '466.0/16060::Nevada'
   '96.0/16060::Fresno+St.'
   '626.0/16060::San+Diego+St.'
   '630.0/16060::San+Jose+St.'
   '473.0/16060::New+Mexico'
   '721.0/16060::Air+Force'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
