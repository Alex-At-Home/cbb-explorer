#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=mac
array=(
   '71.0/16061::Bowling+Green'
   '86.0/16061::Buffalo'
   '331.0/16061::Kent+St.'
   '129.0/16061::Central+Mich.'
   '5.0/16061::Akron'
   '519.0/16061::Ohio'
   '709.0/16061::Toledo'
   '47.0/16061::Ball+St.'
   '204.0/16061::Eastern+Mich.'
   '414.0/16061::Miami+%28OH%29'
   '774.0/16061::Western+Mich.'
   '503.0/16061::NIU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
