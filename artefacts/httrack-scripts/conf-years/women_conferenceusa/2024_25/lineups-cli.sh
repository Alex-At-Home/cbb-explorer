#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=conferenceusa
array=(
   '231.0/16720::FIU'
   '772.0/16720::Western+Ky.'
   '366.0/16720::Louisiana+Tech'
   '704.0/16720::UTEP'
   '419.0/16720::Middle+Tenn.'
   '315.0/16720::Jacksonville+St.'
   '355.0/16720::Liberty'
   '472.0/16720::New+Mexico+St.'
   '624.0/16720::Sam+Houston'
   '1157.0/16720::Kennesaw+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"