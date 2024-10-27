#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=summit
array=(
   '527.0/16700::Oral+Roberts'
   '649.0/16700::South+Dakota+St.'
   '2707.0/16700::Kansas+City'
   '650.0/16700::South+Dakota'
   '771.0/16700::Western+Ill.'
   '183.0/16700::Denver'
   '493.0/16700::North+Dakota+St.'
   '464.0/16700::Omaha'
   '494.0/16700::North+Dakota'
   '620.0/16700::St.+Thomas+%28MN%29'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"