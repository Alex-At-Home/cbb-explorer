#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=summit
array=(
   '527.0/15881::Oral+Roberts'
   '649.0/15881::South+Dakota+St.'
   '2707.0/15881::Kansas+City'
   '650.0/15881::South+Dakota'
   '771.0/15881::Western+Ill.'
   '183.0/15881::Denver'
   '493.0/15881::North+Dakota+St.'
   '464.0/15881::Omaha'
   '494.0/15881::North+Dakota'
   '620.0/15881::St.+Thomas+%28MN%29'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
