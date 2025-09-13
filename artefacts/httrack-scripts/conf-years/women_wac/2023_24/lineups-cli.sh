#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=wac
array=(
   '1104.0/16500::Grand+Canyon'
   '30024.0/16500::Utah+Valley'
   '536.0/16500::UTRGV'
   '1356.0/16500::Seattle+U'
   '136.0/16500::Chicago+St.'
   '676.0/16500::SFA'
   '2.0/16500::Abilene+Christian'
   '30135.0/16500::California+Baptist'
   '1395.0/16500::Tarleton+St.'
   '30095.0/16500::Utah+Tech'
   '667.0/16500::Southern+Utah'
   '702.0/16500::UT+Arlington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"