#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=bigeast
array=(
   '169.0/15061::Creighton'
   '387.0/15061::Marquette'
   '635.0/15061::Seton+Hall'
   '251.0/15061::Georgetown'
   '603.0/15061::St.+John%27s+%28NY%29'
   '739.0/15061::Villanova'
   '556.0/15061::Providence'
   '176.0/15061::DePaul'
   '812.0/15061::Xavier'
   '87.0/15061::Butler'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
