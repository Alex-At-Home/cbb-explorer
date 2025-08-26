#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=conferenceusa
array=(
   '229.0/15480::Fla.+Atlantic'
   '231.0/15480::FIU'
   '574.0/15480::Rice'
   '388.0/15480::Marshall'
   '706.0/15480::UTSA'
   '497.0/15480::North+Texas'
   '9.0/15480::UAB'
   '772.0/15480::Western+Ky.'
   '366.0/15480::Louisiana+Tech'
   '704.0/15480::UTEP'
   '523.0/15480::Old+Dominion'
   '664.0/15480::Southern+Miss.'
   '458.0/15480::Charlotte'
   '419.0/15480::Middle+Tenn.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
