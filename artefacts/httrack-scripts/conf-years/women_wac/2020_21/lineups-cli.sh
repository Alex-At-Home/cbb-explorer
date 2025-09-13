#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=wac
array=(
   '1104.0/15500::Grand+Canyon'
   '472.0/15500::New+Mexico+St.'
   '30024.0/15500::Utah+Valley'
   '536.0/15500::UTRGV'
   '1356.0/15500::Seattle+U'
   '136.0/15500::Chicago+St.'
   '30135.0/15500::California+Baptist'
   '1395.0/15500::Tarleton+St.'
   '30095.0/15500::Dixie+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
