#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=bigeast
array=(
   '169.0/15480::Creighton'
   '739.0/15480::Villanova'
   '603.0/15480::St.+John%27s+%28NY%29'
   '812.0/15480::Xavier'
   '635.0/15480::Seton+Hall'
   '556.0/15480::Providence'
   '176.0/15480::DePaul'
   '164.0/15480::UConn'
   '387.0/15480::Marquette'
   '251.0/15480::Georgetown'
   '87.0/15480::Butler'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
