#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=women_bigeast
array=(
   '164.0/15866::UConn'
   '176.0/15866::DePaul'
   '635.0/15866::Seton+Hall'
   '739.0/15866::Villanova'
   '387.0/15866::Marquette'
   '603.0/15866::St.+John%27s+%28NY%29'
   '812.0/15866::Xavier'
   '169.0/15866::Creighton'
   '556.0/15866::Providence'
   '87.0/15866::Butler'
   '251.0/15866::Georgetown'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
