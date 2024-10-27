#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=colonial
array=(
   '460.0/16700::UNCW'
   '283.0/16700::Hofstra'
   '191.0/16700::Drexel'
   '1014.0/16700::Col.+of+Charleston'
   '500.0/16700::Northeastern'
   '711.0/16700::Towson'
   '1068.0/16700::Elon'
   '180.0/16700::Delaware'
   '786.0/16700::William+%26+Mary'
   '270.0/16700::Hampton'
   '439.0/16700::Monmouth'
   '488.0/16700::N.C.+A%26T'
   '683.0/16700::Stony+Brook'
   '115.0/16700::Campbell'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"