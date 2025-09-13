#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=mac
array=(
   '71.0/16720::Bowling+Green'
   '86.0/16720::Buffalo'
   '331.0/16720::Kent+St.'
   '129.0/16720::Central+Mich.'
   '5.0/16720::Akron'
   '519.0/16720::Ohio'
   '709.0/16720::Toledo'
   '47.0/16720::Ball+St.'
   '204.0/16720::Eastern+Mich.'
   '414.0/16720::Miami+%28OH%29'
   '774.0/16720::Western+Mich.'
   '503.0/16720::NIU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"