#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=bigeast
array=(
   '251.0/14300::Georgetown'
   '176.0/14300::DePaul'
   '169.0/14300::Creighton'
   '387.0/14300::Marquette'
   '603.0/14300::St.+John%27s+%28NY%29'
   '635.0/14300::Seton+Hall'
   '739.0/14300::Villanova'
   '812.0/14300::Xavier'
   '87.0/14300::Butler'
   '556.0/14300::Providence'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
