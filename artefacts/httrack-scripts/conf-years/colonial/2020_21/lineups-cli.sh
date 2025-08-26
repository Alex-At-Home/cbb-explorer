#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=colonial
array=(
   '317.0/15480::James+Madison'
   '460.0/15480::UNCW'
   '283.0/15480::Hofstra'
   '191.0/15480::Drexel'
   '1014.0/15480::Col.+of+Charleston'
   '500.0/15480::Northeastern'
   '711.0/15480::Towson'
   '1068.0/15480::Elon'
   '180.0/15480::Delaware'
   '786.0/15480::William+%26+Mary'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
