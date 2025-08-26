#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=acc
array=(
   '457.0/14300::North+Carolina'
   '193.0/14300::Duke'
   '490.0/14300::NC+State'
   '234.0/14300::Florida+St.'
   '367.0/14300::Louisville'
   '742.0/14300::Virginia+Tech'
   '415.0/14300::Miami+%28FL%29'
   '746.0/14300::Virginia'
   '67.0/14300::Boston+College'
   '545.0/14300::Pittsburgh'
   '688.0/14300::Syracuse'
   '147.0/14300::Clemson'
   '513.0/14300::Notre+Dame'
   '749.0/14300::Wake+Forest'
   '255.0/14300::Georgia+Tech'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
