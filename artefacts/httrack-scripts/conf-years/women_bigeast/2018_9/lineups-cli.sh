#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=women_bigeast
array=(
   '387.0/14320::Marquette'
   '176.0/14320::DePaul'
   '635.0/14320::Seton+Hall'
   '87.0/14320::Butler'
   '169.0/14320::Creighton'
   '603.0/14320::St.+John%27s+%28NY%29'
   '739.0/14320::Villanova'
   '556.0/14320::Providence'
   '812.0/14320::Xavier'
   '251.0/14320::Georgetown'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
