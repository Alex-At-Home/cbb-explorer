#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=women_acc
array=(
   '490.0/16720::NC+State'
   '367.0/16720::Louisville'
   '457.0/16720::North+Carolina'
   '193.0/16720::Duke'
   '742.0/16720::Virginia+Tech'
   '688.0/16720::Syracuse'
   '147.0/16720::Clemson'
   '513.0/16720::Notre+Dame'
   '67.0/16720::Boston+College'
   '749.0/16720::Wake+Forest'
   '415.0/16720::Miami+%28FL%29'
   '255.0/16720::Georgia+Tech'
   '545.0/16720::Pittsburgh'
   '234.0/16720::Florida+St.'
   '746.0/16720::Virginia'
   '674.0/16720::Stanford'
   '107.0/16720::California'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"