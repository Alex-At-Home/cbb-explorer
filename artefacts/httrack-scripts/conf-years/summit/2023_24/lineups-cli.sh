#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=summit
array=(
   '527.0/16501::Oral+Roberts'
   '649.0/16501::South+Dakota+St.'
   '2707.0/16501::Kansas+City'
   '650.0/16501::South+Dakota'
   '771.0/16501::Western+Ill.'
   '183.0/16501::Denver'
   '493.0/16501::North+Dakota+St.'
   '464.0/16501::Omaha'
   '494.0/16501::North+Dakota'
   '620.0/16501::St.+Thomas+%28MN%29'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"