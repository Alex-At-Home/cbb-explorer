#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=wac
array=(
   '1104.0/16061::Grand+Canyon'
   '472.0/16061::New+Mexico+St.'
   '30024.0/16061::Utah+Valley'
   '536.0/16061::UTRGV'
   '1356.0/16061::Seattle+U'
   '136.0/16061::Chicago+St.'
   '676.0/16061::SFA'
   '2.0/16061::Abilene+Christian'
   '624.0/16061::Sam+Houston'
   '30135.0/16061::California+Baptist'
   '1395.0/16061::Tarleton+St.'
   '346.0/16061::Lamar+University'
   '30095.0/16061::Utah+Tech'
   '667.0/16061::Southern+Utah'
   '702.0/16061::UT+Arlington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
