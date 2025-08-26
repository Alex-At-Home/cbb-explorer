#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=wac
array=(
   '1104.0/15480::Grand+Canyon'
   '472.0/15480::New+Mexico+St.'
   '30024.0/15480::Utah+Valley'
   '536.0/15480::UTRGV'
   '1356.0/15480::Seattle+U'
   '136.0/15480::Chicago+St.'
   '30135.0/15480::California+Baptist'
   '1395.0/15480::Tarleton+St.'
   '30095.0/15480::Dixie+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
