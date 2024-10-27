#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=mac
array=(
   '71.0/16501::Bowling+Green'
   '86.0/16501::Buffalo'
   '331.0/16501::Kent+St.'
   '129.0/16501::Central+Mich.'
   '5.0/16501::Akron'
   '519.0/16501::Ohio'
   '709.0/16501::Toledo'
   '47.0/16501::Ball+St.'
   '204.0/16501::Eastern+Mich.'
   '414.0/16501::Miami+%28OH%29'
   '774.0/16501::Western+Mich.'
   '503.0/16501::NIU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"