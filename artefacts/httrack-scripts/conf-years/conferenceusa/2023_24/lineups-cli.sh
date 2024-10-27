#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=conferenceusa
array=(
   '231.0/16501::FIU'
   '772.0/16501::Western+Ky.'
   '366.0/16501::Louisiana+Tech'
   '704.0/16501::UTEP'
   '419.0/16501::Middle+Tenn.'
   '315.0/16501::Jacksonville+St.'
   '355.0/16501::Liberty'
   '472.0/16501::New+Mexico+St.'
   '624.0/16501::Sam+Houston'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"