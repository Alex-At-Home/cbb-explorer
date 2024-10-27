#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=wac
array=(
   '1104.0/16700::Grand+Canyon'
   '30024.0/16700::Utah+Valley'
   '1356.0/16700::Seattle+U'
   '676.0/16700::SFA'
   '2.0/16700::Abilene+Christian'
   '30135.0/16700::California+Baptist'
   '1395.0/16700::Tarleton+St.'
   '30095.0/16700::Utah+Tech'
   '667.0/16700::Southern+Utah'
   '702.0/16700::UT+Arlington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"