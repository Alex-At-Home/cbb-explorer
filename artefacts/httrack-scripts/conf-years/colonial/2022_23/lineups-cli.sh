#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=colonial
array=(
   '460.0/16060::UNCW'
   '283.0/16060::Hofstra'
   '191.0/16060::Drexel'
   '1014.0/16060::Col.+of+Charleston'
   '500.0/16060::Northeastern'
   '711.0/16060::Towson'
   '1068.0/16060::Elon'
   '180.0/16060::Delaware'
   '786.0/16060::William+%26+Mary'
   '270.0/16060::Hampton'
   '439.0/16060::Monmouth'
   '488.0/16060::N.C.+A%26T'
   '683.0/16060::Stony+Brook'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
