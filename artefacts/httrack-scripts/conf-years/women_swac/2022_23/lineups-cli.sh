#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=swac
array=(
   '6.0/16061::Alabama+A%26M'
   '699.0/16061::Texas+Southern'
   '665.0/16061::Southern+U.'
   '553.0/16061::Prairie+View'
   '17.0/16061::Alcorn'
   '261.0/16061::Grambling'
   '2678.0/16061::Ark.-Pine+Bluff'
   '7.0/16061::Alabama+St.'
   '432.0/16061::Mississippi+Val.'
   '314.0/16061::Jackson+St.'
   '228.0/16061::Florida+A%26M'
   '61.0/16061::Bethune-Cookman'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
