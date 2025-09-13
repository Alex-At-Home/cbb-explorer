#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=colonial
array=(
   '460.0/16061::UNCW'
   '283.0/16061::Hofstra'
   '191.0/16061::Drexel'
   '1014.0/16061::Col.+of+Charleston'
   '500.0/16061::Northeastern'
   '711.0/16061::Towson'
   '1068.0/16061::Elon'
   '180.0/16061::Delaware'
   '786.0/16061::William+%26+Mary'
   '270.0/16061::Hampton'
   '439.0/16061::Monmouth'
   '488.0/16061::N.C.+A%26T'
   '683.0/16061::Stony+Brook'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
