#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=wac
array=(
   '1104.0/15881::Grand+Canyon'
   '472.0/15881::New+Mexico+St.'
   '30024.0/15881::Utah+Valley'
   '536.0/15881::UTRGV'
   '1356.0/15881::Seattle+U'
   '136.0/15881::Chicago+St.'
   '676.0/15881::SFA'
   '2.0/15881::Abilene+Christian'
   '624.0/15881::Sam+Houston'
   '30135.0/15881::California+Baptist'
   '1395.0/15881::Tarleton+St.'
   '346.0/15881::Lamar+University'
   '30095.0/15881::Dixie+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
