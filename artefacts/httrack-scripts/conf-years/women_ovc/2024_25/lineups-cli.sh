#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=ovc
array=(
   '201.0/16720::Eastern+Ill.'
   '654.0/16720::Southeast+Mo.+St.'
   '691.0/16720::Tennessee+St.'
   '695.0/16720::UT+Martin'
   '660.0/16720::SIUE'
   '444.0/16720::Morehead+St.'
   '692.0/16720::Tennessee+Tech'
   '32.0/16720::Little+Rock'
   '30136.0/16720::Lindenwood'
   '661.0/16720::Southern+Ind.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"