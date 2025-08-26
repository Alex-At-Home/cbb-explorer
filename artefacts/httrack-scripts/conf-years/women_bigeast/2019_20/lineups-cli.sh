#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=women_bigeast
array=(
   '176.0/15002::DePaul'
   '603.0/15002::St.+John%27s+%28NY%29'
   '635.0/15002::Seton+Hall'
   '387.0/15002::Marquette'
   '169.0/15002::Creighton'
   '87.0/15002::Butler'
   '739.0/15002::Villanova'
   '812.0/15002::Xavier'
   '556.0/15002::Providence'
   '251.0/15002::Georgetown'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
