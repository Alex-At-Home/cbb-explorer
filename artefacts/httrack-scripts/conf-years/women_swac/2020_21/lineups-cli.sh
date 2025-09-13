#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=swac
array=(
   '6.0/15500::Alabama+A%26M'
   '699.0/15500::Texas+Southern'
   '665.0/15500::Southern+U.'
   '553.0/15500::Prairie+View'
   '17.0/15500::Alcorn'
   '261.0/15500::Grambling'
   '2678.0/15500::Ark.-Pine+Bluff'
   '7.0/15500::Alabama+St.'
   '432.0/15500::Mississippi+Val.'
   '314.0/15500::Jackson+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
