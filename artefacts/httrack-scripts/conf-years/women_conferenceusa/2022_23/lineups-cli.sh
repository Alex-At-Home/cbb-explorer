#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=conferenceusa
array=(
   '229.0/16061::Fla.+Atlantic'
   '231.0/16061::FIU'
   '574.0/16061::Rice'
   '706.0/16061::UTSA'
   '497.0/16061::North+Texas'
   '9.0/16061::UAB'
   '772.0/16061::Western+Ky.'
   '366.0/16061::Louisiana+Tech'
   '704.0/16061::UTEP'
   '458.0/16061::Charlotte'
   '419.0/16061::Middle+Tenn.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
