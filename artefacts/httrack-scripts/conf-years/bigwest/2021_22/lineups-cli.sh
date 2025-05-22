#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=bigwest
array=(
   '108.0/15881::UC+Davis'
   '109.0/15881::UC+Irvine'
   '104.0/15881::UC+Santa+Barbara'
   '97.0/15881::Cal+St.+Fullerton'
   '277.0/15881::Hawaii'
   '101.0/15881::CSUN'
   '99.0/15881::Long+Beach+St.'
   '94.0/15881::CSU+Bakersfield'
   '111.0/15881::UC+Riverside'
   '90.0/15881::Cal+Poly'
   '112.0/15881::UC+San+Diego'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
