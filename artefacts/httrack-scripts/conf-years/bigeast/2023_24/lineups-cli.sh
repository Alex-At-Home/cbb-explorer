#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=bigeast
array=(
   '169.0/16501::Creighton'
   '739.0/16501::Villanova'
   '603.0/16501::St.+John%27s+%28NY%29'
   '812.0/16501::Xavier'
   '635.0/16501::Seton+Hall'
   '556.0/16501::Providence'
   '176.0/16501::DePaul'
   '164.0/16501::UConn'
   '387.0/16501::Marquette'
   '251.0/16501::Georgetown'
   '87.0/16501::Butler'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
