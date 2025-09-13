#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=swac
array=(
   '6.0/16720::Alabama+A%26M'
   '699.0/16720::Texas+Southern'
   '665.0/16720::Southern+U.'
   '553.0/16720::Prairie+View'
   '17.0/16720::Alcorn'
   '261.0/16720::Grambling'
   '2678.0/16720::Ark.-Pine+Bluff'
   '7.0/16720::Alabama+St.'
   '432.0/16720::Mississippi+Val.'
   '314.0/16720::Jackson+St.'
   '228.0/16720::Florida+A%26M'
   '61.0/16720::Bethune-Cookman'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"