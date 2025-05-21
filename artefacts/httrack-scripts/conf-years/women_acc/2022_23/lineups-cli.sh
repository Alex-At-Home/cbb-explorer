#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=women_acc
array=(
   '490.0/16061::NC+State'
   '367.0/16061::Louisville'
   '457.0/16061::North+Carolina'
   '193.0/16061::Duke'
   '742.0/16061::Virginia+Tech'
   '688.0/16061::Syracuse'
   '147.0/16061::Clemson'
   '513.0/16061::Notre+Dame'
   '67.0/16061::Boston+College'
   '749.0/16061::Wake+Forest'
   '415.0/16061::Miami+%28FL%29'
   '255.0/16061::Georgia+Tech'
   '545.0/16061::Pittsburgh'
   '234.0/16061::Florida+St.'
   '746.0/16061::Virginia'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
