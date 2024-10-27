#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=women_misc_conf
array=(
  '260.0/16720::Gonzaga'
  '554.0/16720::Princeton'
  '649.0/16720::South+Dakota+St.'
  '650.0/16720::South+Dakota'
  '669.0/16720::Missouri+St.'
  '28755.0/16720::FGCU'
  '528.0/16720::Oregon+St.'
  '754.0/16720::Washington+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"