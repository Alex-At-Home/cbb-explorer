#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=colonial
array=(
   '460.0/16501::UNCW'
   '283.0/16501::Hofstra'
   '191.0/16501::Drexel'
   '1014.0/16501::Col.+of+Charleston'
   '500.0/16501::Northeastern'
   '711.0/16501::Towson'
   '1068.0/16501::Elon'
   '180.0/16501::Delaware'
   '786.0/16501::William+%26+Mary'
   '270.0/16501::Hampton'
   '439.0/16501::Monmouth'
   '488.0/16501::N.C.+A%26T'
   '683.0/16501::Stony+Brook'
   '115.0/16501::Campbell'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"