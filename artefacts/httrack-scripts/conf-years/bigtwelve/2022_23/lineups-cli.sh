#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=bigtwelve
array=(
   '51.0/16060::Baylor'
   '522.0/16060::Oklahoma'
   '521.0/16060::Oklahoma+St.'
   '328.0/16060::Kansas'
   '703.0/16060::Texas'
   '768.0/16060::West+Virginia'
   '700.0/16060::Texas+Tech'
   '311.0/16060::Iowa+St.'
   '698.0/16060::TCU'
   '327.0/16060::Kansas+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
