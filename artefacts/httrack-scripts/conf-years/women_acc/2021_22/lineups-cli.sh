#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=women_acc
array=(
   '490.0/15866::NC+State'
   '367.0/15866::Louisville'
   '457.0/15866::North+Carolina'
   '193.0/15866::Duke'
   '742.0/15866::Virginia+Tech'
   '688.0/15866::Syracuse'
   '147.0/15866::Clemson'
   '513.0/15866::Notre+Dame'
   '67.0/15866::Boston+College'
   '749.0/15866::Wake+Forest'
   '415.0/15866::Miami+%28FL%29'
   '255.0/15866::Georgia+Tech'
   '545.0/15866::Pittsburgh'
   '234.0/15866::Florida+St.'
   '746.0/15866::Virginia'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
