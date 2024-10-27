#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=ovc
array=(
   '201.0/16501::Eastern+Ill.'
   '654.0/16501::Southeast+Mo.+St.'
   '691.0/16501::Tennessee+St.'
   '695.0/16501::UT+Martin'
   '660.0/16501::SIUE'
   '444.0/16501::Morehead+St.'
   '692.0/16501::Tennessee+Tech'
   '32.0/16501::Little+Rock'
   '30136.0/16501::Lindenwood'
   '661.0/16501::Southern+Ind.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"