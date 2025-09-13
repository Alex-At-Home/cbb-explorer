#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=wac
array=(
   '1104.0/16720::Grand+Canyon'
   '30024.0/16720::Utah+Valley'
   '1356.0/16720::Seattle+U'
   '676.0/16720::SFA'
   '2.0/16720::Abilene+Christian'
   '30135.0/16720::California+Baptist'
   '1395.0/16720::Tarleton+St.'
   '30095.0/16720::Utah+Tech'
   '667.0/16720::Southern+Utah'
   '702.0/16720::UT+Arlington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"