#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=ovc
array=(
   '202.0/15480::Eastern+Ky.'
   '14927.0/15480::Belmont'
   '454.0/15480::Murray+St.'
   '43.0/15480::Austin+Peay'
   '315.0/15480::Jacksonville+St.'
   '201.0/15480::Eastern+Ill.'
   '654.0/15480::Southeast+Mo.+St.'
   '691.0/15480::Tennessee+St.'
   '695.0/15480::UT+Martin'
   '660.0/15480::SIUE'
   '444.0/15480::Morehead+St.'
   '692.0/15480::Tennessee+Tech'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
