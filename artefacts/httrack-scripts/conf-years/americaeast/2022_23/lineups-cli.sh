#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=americaeast
array=(
   '738.0/16060::Vermont'
   '391.0/16060::UMBC'
   '14.0/16060::UAlbany'
   '368.0/16060::UMass+Lowell'
   '469.0/16060::New+Hampshire'
   '471.0/16060::NJIT'
   '272.0/16060::Hartford'
   '62.0/16060::Binghamton'
   '380.0/16060::Maine'
   '81.0/16060::Bryant'
   '682.0/16060::Stonehill'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
