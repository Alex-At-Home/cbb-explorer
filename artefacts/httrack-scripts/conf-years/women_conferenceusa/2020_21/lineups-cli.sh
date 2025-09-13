#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=conferenceusa
array=(
   '229.0/15500::Fla.+Atlantic'
   '231.0/15500::FIU'
   '574.0/15500::Rice'
   '388.0/15500::Marshall'
   '706.0/15500::UTSA'
   '497.0/15500::North+Texas'
   '9.0/15500::UAB'
   '772.0/15500::Western+Ky.'
   '366.0/15500::Louisiana+Tech'
   '704.0/15500::UTEP'
   '523.0/15500::Old+Dominion'
   '664.0/15500::Southern+Miss.'
   '458.0/15500::Charlotte'
   '419.0/15500::Middle+Tenn.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
