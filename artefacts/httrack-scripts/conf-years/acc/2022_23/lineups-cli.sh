#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=acc
array=(
   '255.0/16060::Georgia+Tech'
   '688.0/16060::Syracuse'
   '234.0/16060::Florida+St.'
   '490.0/16060::NC+State'
   '193.0/16060::Duke'
   '742.0/16060::Virginia+Tech'
   '545.0/16060::Pittsburgh'
   '746.0/16060::Virginia'
   '457.0/16060::North+Carolina'
   '67.0/16060::Boston+College'
   '367.0/16060::Louisville'
   '513.0/16060::Notre+Dame'
   '749.0/16060::Wake+Forest'
   '415.0/16060::Miami+%28FL%29'
   '147.0/16060::Clemson'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
