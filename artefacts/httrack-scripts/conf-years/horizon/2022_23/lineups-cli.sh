#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=horizon
array=(
   '810.0/16060::Wright+St.'
   '308.0/16060::Purdue+Fort+Wayne'
   '817.0/16060::Youngstown+St.'
   '797.0/16060::Milwaukee'
   '184.0/16060::Detroit+Mercy'
   '579.0/16060::Robert+Morris'
   '514.0/16060::Oakland'
   '794.0/16060::Green+Bay'
   '505.0/16060::Northern+Ky.'
   '148.0/16060::Cleveland+St.'
   '2699.0/16060::IUPUI'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
