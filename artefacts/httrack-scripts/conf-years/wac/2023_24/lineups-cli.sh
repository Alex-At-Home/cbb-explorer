#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=wac
array=(
   '1104.0/16501::Grand+Canyon'
   '30024.0/16501::Utah+Valley'
   '536.0/16501::UTRGV'
   '1356.0/16501::Seattle+U'
   '136.0/16501::Chicago+St.'
   '676.0/16501::SFA'
   '2.0/16501::Abilene+Christian'
   '30135.0/16501::California+Baptist'
   '1395.0/16501::Tarleton+St.'
   '30095.0/16501::Utah+Tech'
   '667.0/16501::Southern+Utah'
   '702.0/16501::UT+Arlington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"