#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=conferenceusa
array=(
   '229.0/16060::Fla.+Atlantic'
   '231.0/16060::FIU'
   '574.0/16060::Rice'
   '706.0/16060::UTSA'
   '497.0/16060::North+Texas'
   '9.0/16060::UAB'
   '772.0/16060::Western+Ky.'
   '366.0/16060::Louisiana+Tech'
   '704.0/16060::UTEP'
   '458.0/16060::Charlotte'
   '419.0/16060::Middle+Tenn.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
