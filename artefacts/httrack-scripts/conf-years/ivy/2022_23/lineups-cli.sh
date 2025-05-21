#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=ivy
array=(
   '813.0/16060::Yale'
   '80.0/16060::Brown'
   '158.0/16060::Columbia'
   '167.0/16060::Cornell'
   '275.0/16060::Harvard'
   '172.0/16060::Dartmouth'
   '540.0/16060::Penn'
   '554.0/16060::Princeton'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
