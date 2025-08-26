#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=acc
array=(
   '193.0/15061::Duke'
   '234.0/15061::Florida+St.'
   '490.0/15061::NC+State'
   '688.0/15061::Syracuse'
   '513.0/15061::Notre+Dame'
   '367.0/15061::Louisville'
   '749.0/15061::Wake+Forest'
   '457.0/15061::North+Carolina'
   '415.0/15061::Miami+%28FL%29'
   '255.0/15061::Georgia+Tech'
   '742.0/15061::Virginia+Tech'
   '147.0/15061::Clemson'
   '545.0/15061::Pittsburgh'
   '67.0/15061::Boston+College'
   '746.0/15061::Virginia'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
