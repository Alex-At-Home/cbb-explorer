#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=mac
array=(
   '71.0/16500::Bowling+Green'
   '86.0/16500::Buffalo'
   '331.0/16500::Kent+St.'
   '129.0/16500::Central+Mich.'
   '5.0/16500::Akron'
   '519.0/16500::Ohio'
   '709.0/16500::Toledo'
   '47.0/16500::Ball+St.'
   '204.0/16500::Eastern+Mich.'
   '414.0/16500::Miami+%28OH%29'
   '774.0/16500::Western+Mich.'
   '503.0/16500::NIU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"