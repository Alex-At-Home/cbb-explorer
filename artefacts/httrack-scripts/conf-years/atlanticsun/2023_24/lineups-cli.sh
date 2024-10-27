#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=atlanticsun
array=(
   '28755.0/16501::FGCU'
   '28600.0/16501::Lipscomb'
   '316.0/16501::Jacksonville'
   '2711.0/16501::North+Florida'
   '678.0/16501::Stetson'
   '52.0/16501::Bellarmine'
   '487.0/16501::North+Ala.'
   '1004.0/16501::Central+Ark.'
   '202.0/16501::Eastern+Ky.'
   '43.0/16501::Austin+Peay'
   '11504.0/16501::Queens+%28NC%29'
   '1157.0/16501::Kennesaw+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"