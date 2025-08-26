#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=acc
array=(
   '255.0/15480::Georgia+Tech'
   '688.0/15480::Syracuse'
   '234.0/15480::Florida+St.'
   '490.0/15480::NC+State'
   '193.0/15480::Duke'
   '742.0/15480::Virginia+Tech'
   '545.0/15480::Pittsburgh'
   '746.0/15480::Virginia'
   '457.0/15480::North+Carolina'
   '67.0/15480::Boston+College'
   '367.0/15480::Louisville'
   '513.0/15480::Notre+Dame'
   '749.0/15480::Wake+Forest'
   '415.0/15480::Miami+%28FL%29'
   '147.0/15480::Clemson'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
