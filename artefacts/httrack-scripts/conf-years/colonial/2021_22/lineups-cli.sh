#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=colonial
array=(
   '317.0/15881::James+Madison'
   '460.0/15881::UNCW'
   '283.0/15881::Hofstra'
   '191.0/15881::Drexel'
   '1014.0/15881::Col.+of+Charleston'
   '500.0/15881::Northeastern'
   '711.0/15881::Towson'
   '1068.0/15881::Elon'
   '180.0/15881::Delaware'
   '786.0/15881::William+%26+Mary'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
