#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=atlanticsun
array=(
   '28755.0/16720::FGCU'
   '28600.0/16720::Lipscomb'
   '316.0/16720::Jacksonville'
   '2711.0/16720::North+Florida'
   '678.0/16720::Stetson'
   '52.0/16720::Bellarmine'
   '487.0/16720::North+Ala.'
   '1004.0/16720::Central+Ark.'
   '202.0/16720::Eastern+Ky.'
   '43.0/16720::Austin+Peay'
   '11504.0/16720::Queens+%28NC%29'
   '766.0/16720::West+Ga.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"