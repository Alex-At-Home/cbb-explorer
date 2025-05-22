#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=swac
array=(
   '6.0/15881::Alabama+A%26M'
   '699.0/15881::Texas+Southern'
   '665.0/15881::Southern+U.'
   '553.0/15881::Prairie+View'
   '17.0/15881::Alcorn'
   '261.0/15881::Grambling'
   '2678.0/15881::Ark.-Pine+Bluff'
   '7.0/15881::Alabama+St.'
   '432.0/15881::Mississippi+Val.'
   '314.0/15881::Jackson+St.'
   '228.0/15881::Florida+A%26M'
   '61.0/15881::Bethune-Cookman'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
