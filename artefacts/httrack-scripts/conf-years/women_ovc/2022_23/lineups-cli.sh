#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=ovc
array=(
   '201.0/16061::Eastern+Ill.'
   '654.0/16061::Southeast+Mo.+St.'
   '691.0/16061::Tennessee+St.'
   '695.0/16061::UT+Martin'
   '660.0/16061::SIUE'
   '444.0/16061::Morehead+St.'
   '692.0/16061::Tennessee+Tech'
   '32.0/16061::Little+Rock'
   '30136.0/16061::Lindenwood'
   '661.0/16061::Southern+Ind.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
