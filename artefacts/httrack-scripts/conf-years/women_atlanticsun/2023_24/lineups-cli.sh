#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=atlanticsun
array=(
   '28755.0/16500::FGCU'
   '28600.0/16500::Lipscomb'
   '316.0/16500::Jacksonville'
   '2711.0/16500::North+Florida'
   '678.0/16500::Stetson'
   '52.0/16500::Bellarmine'
   '487.0/16500::North+Ala.'
   '1004.0/16500::Central+Ark.'
   '202.0/16500::Eastern+Ky.'
   '43.0/16500::Austin+Peay'
   '11504.0/16500::Queens+%28NC%29'
   '1157.0/16500::Kennesaw+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"