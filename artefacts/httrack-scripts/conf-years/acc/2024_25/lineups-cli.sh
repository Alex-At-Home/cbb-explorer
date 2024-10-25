#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=acc
array=(
   '255.0/16700::Georgia+Tech'
   '688.0/16700::Syracuse'
   '234.0/16700::Florida+St.'
   '490.0/16700::NC+State'
   '193.0/16700::Duke'
   '742.0/16700::Virginia+Tech'
   '545.0/16700::Pittsburgh'
   '746.0/16700::Virginia'
   '457.0/16700::North+Carolina'
   '67.0/16700::Boston+College'
   '367.0/16700::Louisville'
   '513.0/16700::Notre+Dame'
   '749.0/16700::Wake+Forest'
   '415.0/16700::Miami+%28FL%29'
   '147.0/16700::Clemson'
   '107.0/16700::California'
   '674.0/16700::Stanford'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
