#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=acc
array=(
   '255.0/15881::Georgia+Tech'
   '688.0/15881::Syracuse'
   '234.0/15881::Florida+St.'
   '490.0/15881::NC+State'
   '193.0/15881::Duke'
   '742.0/15881::Virginia+Tech'
   '545.0/15881::Pittsburgh'
   '746.0/15881::Virginia'
   '457.0/15881::North+Carolina'
   '67.0/15881::Boston+College'
   '367.0/15881::Louisville'
   '513.0/15881::Notre+Dame'
   '749.0/15881::Wake+Forest'
   '415.0/15881::Miami+%28FL%29'
   '147.0/15881::Clemson'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
