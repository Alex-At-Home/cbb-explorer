#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=ovc
array=(
   '14927.0/15866::Belmont'
   '454.0/15866::Murray+St.'
   '43.0/15866::Austin+Peay'
   '201.0/15866::Eastern+Ill.'
   '654.0/15866::Southeast+Mo.+St.'
   '691.0/15866::Tennessee+St.'
   '695.0/15866::UT+Martin'
   '660.0/15866::SIUE'
   '444.0/15866::Morehead+St.'
   '692.0/15866::Tennessee+Tech'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
