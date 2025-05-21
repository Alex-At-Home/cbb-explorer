#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=sec
array=(
   '365.0/16060::LSU'
   '31.0/16060::Arkansas'
   '8.0/16060::Alabama'
   '257.0/16060::Georgia'
   '694.0/16060::Tennessee'
   '235.0/16060::Florida'
   '37.0/16060::Auburn'
   '430.0/16060::Mississippi+St.'
   '736.0/16060::Vanderbilt'
   '648.0/16060::South+Carolina'
   '433.0/16060::Ole+Miss'
   '434.0/16060::Missouri'
   '334.0/16060::Kentucky'
   '697.0/16060::Texas+A%26M'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
