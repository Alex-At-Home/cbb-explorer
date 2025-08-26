#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=women_acc
array=(
   '513.0/14320::Notre+Dame'
   '367.0/14320::Louisville'
   '688.0/14320::Syracuse'
   '457.0/14320::North+Carolina'
   '415.0/14320::Miami+%28FL%29'
   '67.0/14320::Boston+College'
   '742.0/14320::Virginia+Tech'
   '490.0/14320::NC+State'
   '147.0/14320::Clemson'
   '234.0/14320::Florida+St.'
   '255.0/14320::Georgia+Tech'
   '193.0/14320::Duke'
   '746.0/14320::Virginia'
   '545.0/14320::Pittsburgh'
   '749.0/14320::Wake+Forest'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
