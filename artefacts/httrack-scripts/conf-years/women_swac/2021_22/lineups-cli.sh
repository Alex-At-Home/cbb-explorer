#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=swac
array=(
   '6.0/15866::Alabama+A%26M'
   '699.0/15866::Texas+Southern'
   '665.0/15866::Southern+U.'
   '553.0/15866::Prairie+View'
   '17.0/15866::Alcorn'
   '261.0/15866::Grambling'
   '2678.0/15866::Ark.-Pine+Bluff'
   '7.0/15866::Alabama+St.'
   '432.0/15866::Mississippi+Val.'
   '314.0/15866::Jackson+St.'
   '228.0/15866::Florida+A%26M'
   '61.0/15866::Bethune-Cookman'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
