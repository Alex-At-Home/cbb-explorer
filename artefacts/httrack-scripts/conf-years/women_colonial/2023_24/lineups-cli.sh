#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=colonial
array=(
   '460.0/16500::UNCW'
   '283.0/16500::Hofstra'
   '191.0/16500::Drexel'
   '1014.0/16500::Col.+of+Charleston'
   '500.0/16500::Northeastern'
   '711.0/16500::Towson'
   '1068.0/16500::Elon'
   '180.0/16500::Delaware'
   '786.0/16500::William+%26+Mary'
   '270.0/16500::Hampton'
   '439.0/16500::Monmouth'
   '488.0/16500::N.C.+A%26T'
   '683.0/16500::Stony+Brook'
   '115.0/16500::Campbell'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"