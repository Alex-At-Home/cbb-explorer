#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=conferenceusa
array=(
   '231.0/16500::FIU'
   '772.0/16500::Western+Ky.'
   '366.0/16500::Louisiana+Tech'
   '704.0/16500::UTEP'
   '419.0/16500::Middle+Tenn.'
   '315.0/16500::Jacksonville+St.'
   '355.0/16500::Liberty'
   '472.0/16500::New+Mexico+St.'
   '624.0/16500::Sam+Houston'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"