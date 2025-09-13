#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=ovc
array=(
   '202.0/15500::Eastern+Ky.'
   '14927.0/15500::Belmont'
   '454.0/15500::Murray+St.'
   '43.0/15500::Austin+Peay'
   '315.0/15500::Jacksonville+St.'
   '201.0/15500::Eastern+Ill.'
   '654.0/15500::Southeast+Mo.+St.'
   '691.0/15500::Tennessee+St.'
   '695.0/15500::UT+Martin'
   '660.0/15500::SIUE'
   '444.0/15500::Morehead+St.'
   '692.0/15500::Tennessee+Tech'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
