#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigwest
array=(
   '108.0/16720::UC+Davis'
   '109.0/16720::UC+Irvine'
   '104.0/16720::UC+Santa+Barbara'
   '97.0/16720::Cal+St.+Fullerton'
   '277.0/16720::Hawaii'
   '101.0/16720::CSUN'
   '99.0/16720::Long+Beach+St.'
   '94.0/16720::CSU+Bakersfield'
   '111.0/16720::UC+Riverside'
   '90.0/16720::Cal+Poly'
   '112.0/16720::UC+San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"