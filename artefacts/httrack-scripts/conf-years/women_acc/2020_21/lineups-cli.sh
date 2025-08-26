#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=women_acc
array=(
   '490.0/15500::NC+State'
   '367.0/15500::Louisville'
   '457.0/15500::North+Carolina'
   '193.0/15500::Duke'
   '742.0/15500::Virginia+Tech'
   '688.0/15500::Syracuse'
   '147.0/15500::Clemson'
   '513.0/15500::Notre+Dame'
   '67.0/15500::Boston+College'
   '749.0/15500::Wake+Forest'
   '415.0/15500::Miami+%28FL%29'
   '255.0/15500::Georgia+Tech'
   '545.0/15500::Pittsburgh'
   '234.0/15500::Florida+St.'
   '746.0/15500::Virginia'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
