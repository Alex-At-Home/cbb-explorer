#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigwest
array=(
   '108.0/16700::UC+Davis'
   '109.0/16700::UC+Irvine'
   '104.0/16700::UC+Santa+Barbara'
   '97.0/16700::Cal+St.+Fullerton'
   '277.0/16700::Hawaii'
   '101.0/16700::CSUN'
   '99.0/16700::Long+Beach+St.'
   '94.0/16700::CSU+Bakersfield'
   '111.0/16700::UC+Riverside'
   '90.0/16700::Cal+Poly'
   '112.0/16700::UC+San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"