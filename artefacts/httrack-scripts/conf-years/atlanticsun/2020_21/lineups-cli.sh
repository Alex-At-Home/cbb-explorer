#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=atlanticsun
array=(
   '355.0/15480::Liberty'
   '28755.0/15480::FGCU'
   '28600.0/15480::Lipscomb'
   '316.0/15480::Jacksonville'
   '2711.0/15480::North+Florida'
   '678.0/15480::Stetson'
   '1157.0/15480::Kennesaw+St.'
   '52.0/15480::Bellarmine'
   '487.0/15480::North+Alabama'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
