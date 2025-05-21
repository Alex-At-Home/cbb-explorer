#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=pactwelve
array=(
   '29.0/16060::Arizona'
   '657.0/16060::Southern+California'
   '110.0/16060::UCLA'
   '529.0/16060::Oregon'
   '157.0/16060::Colorado'
   '28.0/16060::Arizona+St.'
   '528.0/16060::Oregon+St.'
   '674.0/16060::Stanford'
   '754.0/16060::Washington+St.'
   '732.0/16060::Utah'
   '107.0/16060::California'
   '756.0/16060::Washington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
