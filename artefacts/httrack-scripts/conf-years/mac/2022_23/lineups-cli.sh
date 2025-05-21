#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=mac
array=(
   '71.0/16060::Bowling+Green'
   '86.0/16060::Buffalo'
   '331.0/16060::Kent+St.'
   '129.0/16060::Central+Mich.'
   '5.0/16060::Akron'
   '519.0/16060::Ohio'
   '709.0/16060::Toledo'
   '47.0/16060::Ball+St.'
   '204.0/16060::Eastern+Mich.'
   '414.0/16060::Miami+%28OH%29'
   '774.0/16060::Western+Mich.'
   '503.0/16060::NIU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
