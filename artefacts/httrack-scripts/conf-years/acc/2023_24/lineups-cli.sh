#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=acc
array=(
   '255.0/16501::Georgia+Tech'
   '688.0/16501::Syracuse'
   '234.0/16501::Florida+St.'
   '490.0/16501::NC+State'
   '193.0/16501::Duke'
   '742.0/16501::Virginia+Tech'
   '545.0/16501::Pittsburgh'
   '746.0/16501::Virginia'
   '457.0/16501::North+Carolina'
   '67.0/16501::Boston+College'
   '367.0/16501::Louisville'
   '513.0/16501::Notre+Dame'
   '749.0/16501::Wake+Forest'
   '415.0/16501::Miami+%28FL%29'
   '147.0/16501::Clemson'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
