#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=atlanticsun
array=(
   '355.0/15500::Liberty'
   '28755.0/15500::FGCU'
   '28600.0/15500::Lipscomb'
   '316.0/15500::Jacksonville'
   '2711.0/15500::North+Florida'
   '678.0/15500::Stetson'
   '1157.0/15500::Kennesaw+St.'
   '52.0/15500::Bellarmine'
   '487.0/15500::North+Alabama'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
