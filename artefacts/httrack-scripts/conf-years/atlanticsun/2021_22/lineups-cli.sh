#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=atlanticsun
array=(
   '355.0/15881::Liberty'
   '28755.0/15881::FGCU'
   '28600.0/15881::Lipscomb'
   '316.0/15881::Jacksonville'
   '2711.0/15881::North+Florida'
   '678.0/15881::Stetson'
   '1157.0/15881::Kennesaw+St.'
   '52.0/15881::Bellarmine'
   '487.0/15881::North+Ala.'
   '1004.0/15881::Central+Ark'
   '202.0/15881::Eastern+Ky.'
   '315.0/15881::Jacksonville+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
