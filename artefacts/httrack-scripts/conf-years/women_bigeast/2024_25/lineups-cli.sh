#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=women_bigeast
array=(
   '164.0/16720::UConn'
   '176.0/16720::DePaul'
   '635.0/16720::Seton+Hall'
   '739.0/16720::Villanova'
   '387.0/16720::Marquette'
   '603.0/16720::St.+John%27s+%28NY%29'
   '812.0/16720::Xavier'
   '169.0/16720::Creighton'
   '556.0/16720::Providence'
   '87.0/16720::Butler'
   '251.0/16720::Georgetown'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"