#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=women_acc
array=(
   '67.0/15002::Boston+College'
   '457.0/15002::North+Carolina'
   '367.0/15002::Louisville'
   '234.0/15002::Florida+St.'
   '490.0/15002::NC+State'
   '742.0/15002::Virginia+Tech'
   '688.0/15002::Syracuse'
   '193.0/15002::Duke'
   '749.0/15002::Wake+Forest'
   '513.0/15002::Notre+Dame'
   '415.0/15002::Miami+%28FL%29'
   '255.0/15002::Georgia+Tech'
   '746.0/15002::Virginia'
   '147.0/15002::Clemson'
   '545.0/15002::Pittsburgh'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
