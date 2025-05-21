#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=swac
array=(
   '6.0/16060::Alabama+A%26M'
   '699.0/16060::Texas+Southern'
   '665.0/16060::Southern+U.'
   '553.0/16060::Prairie+View'
   '17.0/16060::Alcorn'
   '261.0/16060::Grambling'
   '2678.0/16060::Ark.-Pine+Bluff'
   '7.0/16060::Alabama+St.'
   '432.0/16060::Mississippi+Val.'
   '314.0/16060::Jackson+St.'
   '228.0/16060::Florida+A%26M'
   '61.0/16060::Bethune-Cookman'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
