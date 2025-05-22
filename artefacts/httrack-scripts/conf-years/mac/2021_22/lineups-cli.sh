#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=mac
array=(
   '71.0/15881::Bowling+Green'
   '86.0/15881::Buffalo'
   '331.0/15881::Kent+St.'
   '129.0/15881::Central+Mich.'
   '5.0/15881::Akron'
   '519.0/15881::Ohio'
   '709.0/15881::Toledo'
   '47.0/15881::Ball+St.'
   '204.0/15881::Eastern+Mich.'
   '414.0/15881::Miami+%28OH%29'
   '774.0/15881::Western+Mich.'
   '503.0/15881::Northern+Ill.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
