#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=bigeast
array=(
   '169.0/15881::Creighton'
   '739.0/15881::Villanova'
   '603.0/15881::St.+John%27s+%28NY%29'
   '812.0/15881::Xavier'
   '635.0/15881::Seton+Hall'
   '556.0/15881::Providence'
   '176.0/15881::DePaul'
   '164.0/15881::UConn'
   '387.0/15881::Marquette'
   '251.0/15881::Georgetown'
   '87.0/15881::Butler'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
