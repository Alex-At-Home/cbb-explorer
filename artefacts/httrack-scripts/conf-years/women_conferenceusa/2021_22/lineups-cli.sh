#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=conferenceusa
array=(
   '229.0/15866::Fla.+Atlantic'
   '231.0/15866::FIU'
   '574.0/15866::Rice'
   '388.0/15866::Marshall'
   '706.0/15866::UTSA'
   '497.0/15866::North+Texas'
   '9.0/15866::UAB'
   '772.0/15866::Western+Ky.'
   '366.0/15866::Louisiana+Tech'
   '704.0/15866::UTEP'
   '523.0/15866::Old+Dominion'
   '664.0/15866::Southern+Miss.'
   '458.0/15866::Charlotte'
   '419.0/15866::Middle+Tenn.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
