#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=colonial
array=(
   '317.0/15866::James+Madison'
   '460.0/15866::UNCW'
   '283.0/15866::Hofstra'
   '191.0/15866::Drexel'
   '1014.0/15866::Col.+of+Charleston'
   '500.0/15866::Northeastern'
   '711.0/15866::Towson'
   '1068.0/15866::Elon'
   '180.0/15866::Delaware'
   '786.0/15866::William+%26+Mary'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
