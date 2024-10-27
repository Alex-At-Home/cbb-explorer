#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=ovc
array=(
   '201.0/16700::Eastern+Ill.'
   '654.0/16700::Southeast+Mo.+St.'
   '691.0/16700::Tennessee+St.'
   '695.0/16700::UT+Martin'
   '660.0/16700::SIUE'
   '444.0/16700::Morehead+St.'
   '692.0/16700::Tennessee+Tech'
   '32.0/16700::Little+Rock'
   '30136.0/16700::Lindenwood'
   '661.0/16700::Southern+Ind.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"