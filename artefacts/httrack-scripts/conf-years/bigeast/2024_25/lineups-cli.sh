#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigeast
array=(
   '169.0/16700::Creighton'
   '739.0/16700::Villanova'
   '603.0/16700::St.+John%27s+%28NY%29'
   '812.0/16700::Xavier'
   '635.0/16700::Seton+Hall'
   '556.0/16700::Providence'
   '176.0/16700::DePaul'
   '164.0/16700::UConn'
   '387.0/16700::Marquette'
   '251.0/16700::Georgetown'
   '87.0/16700::Butler'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
