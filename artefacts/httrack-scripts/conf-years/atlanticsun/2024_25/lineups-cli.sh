#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=atlanticsun
array=(
   '28755.0/16700::FGCU'
   '28600.0/16700::Lipscomb'
   '316.0/16700::Jacksonville'
   '2711.0/16700::North+Florida'
   '678.0/16700::Stetson'
   '52.0/16700::Bellarmine'
   '487.0/16700::North+Ala.'
   '1004.0/16700::Central+Ark.'
   '202.0/16700::Eastern+Ky.'
   '43.0/16700::Austin+Peay'
   '11504.0/16700::Queens+%28NC%29'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"