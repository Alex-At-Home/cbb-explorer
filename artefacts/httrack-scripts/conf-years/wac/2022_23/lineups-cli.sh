#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=wac
array=(
   '1104.0/16060::Grand+Canyon'
   '472.0/16060::New+Mexico+St.'
   '30024.0/16060::Utah+Valley'
   '536.0/16060::UTRGV'
   '1356.0/16060::Seattle+U'
   '136.0/16060::Chicago+St.'
   '676.0/16060::SFA'
   '2.0/16060::Abilene+Christian'
   '624.0/16060::Sam+Houston'
   '30135.0/16060::California+Baptist'
   '1395.0/16060::Tarleton+St.'
   '346.0/16060::Lamar+University'
   '30095.0/16060::Utah+Tech'
   '667.0/16060::Southern+Utah'
   '702.0/16060::UT+Arlington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
