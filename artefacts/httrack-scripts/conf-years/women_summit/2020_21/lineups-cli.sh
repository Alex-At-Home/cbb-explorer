#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=summit
array=(
   '527.0/15500::Oral+Roberts'
   '649.0/15500::South+Dakota+St.'
   '2707.0/15500::Kansas+City'
   '650.0/15500::South+Dakota'
   '771.0/15500::Western+Ill.'
   '183.0/15500::Denver'
   '493.0/15500::North+Dakota+St.'
   '464.0/15500::Omaha'
   '494.0/15500::North+Dakota'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
