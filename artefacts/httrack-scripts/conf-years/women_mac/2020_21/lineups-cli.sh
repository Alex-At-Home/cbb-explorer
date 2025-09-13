#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=mac
array=(
   '71.0/15500::Bowling+Green'
   '86.0/15500::Buffalo'
   '331.0/15500::Kent+St.'
   '129.0/15500::Central+Mich.'
   '5.0/15500::Akron'
   '519.0/15500::Ohio'
   '709.0/15500::Toledo'
   '47.0/15500::Ball+St.'
   '204.0/15500::Eastern+Mich.'
   '414.0/15500::Miami+%28OH%29'
   '774.0/15500::Western+Mich.'
   '503.0/15500::Northern+Ill.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
