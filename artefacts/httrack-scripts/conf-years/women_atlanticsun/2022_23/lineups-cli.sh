#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=atlanticsun
array=(
   '355.0/16061::Liberty'
   '28755.0/16061::FGCU'
   '28600.0/16061::Lipscomb'
   '316.0/16061::Jacksonville'
   '2711.0/16061::North+Florida'
   '678.0/16061::Stetson'
   '1157.0/16061::Kennesaw+St.'
   '52.0/16061::Bellarmine'
   '487.0/16061::North+Ala.'
   '1004.0/16061::Central+Ark.'
   '202.0/16061::Eastern+Ky.'
   '315.0/16061::Jacksonville+St.'
   '43.0/16061::Austin+Peay'
   '11504.0/16061::Queens+%28NC%29'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
