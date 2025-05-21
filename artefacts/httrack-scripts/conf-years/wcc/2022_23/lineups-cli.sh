#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=wcc
array=(
   '260.0/16060::Gonzaga'
   '541.0/16060::Pepperdine'
   '77.0/16060::BYU'
   '629.0/16060::San+Francisco'
   '551.0/16060::Portland'
   '370.0/16060::LMU+%28CA%29'
   '610.0/16060::Saint+Mary%27s+%28CA%29'
   '534.0/16060::Pacific'
   '631.0/16060::Santa+Clara'
   '627.0/16060::San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
