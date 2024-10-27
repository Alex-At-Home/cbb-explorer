#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=women_acc
array=(
   '490.0/16500::NC+State'
   '367.0/16500::Louisville'
   '457.0/16500::North+Carolina'
   '193.0/16500::Duke'
   '742.0/16500::Virginia+Tech'
   '688.0/16500::Syracuse'
   '147.0/16500::Clemson'
   '513.0/16500::Notre+Dame'
   '67.0/16500::Boston+College'
   '749.0/16500::Wake+Forest'
   '415.0/16500::Miami+%28FL%29'
   '255.0/16500::Georgia+Tech'
   '545.0/16500::Pittsburgh'
   '234.0/16500::Florida+St.'
   '746.0/16500::Virginia'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"