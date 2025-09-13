#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=colonial
array=(
   '460.0/16720::UNCW'
   '283.0/16720::Hofstra'
   '191.0/16720::Drexel'
   '1014.0/16720::Col.+of+Charleston'
   '500.0/16720::Northeastern'
   '711.0/16720::Towson'
   '1068.0/16720::Elon'
   '180.0/16720::Delaware'
   '786.0/16720::William+%26+Mary'
   '270.0/16720::Hampton'
   '439.0/16720::Monmouth'
   '488.0/16720::N.C.+A%26T'
   '683.0/16720::Stony+Brook'
   '115.0/16720::Campbell'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"