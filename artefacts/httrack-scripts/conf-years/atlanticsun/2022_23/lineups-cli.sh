#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=atlanticsun
array=(
   '355.0/16060::Liberty'
   '28755.0/16060::FGCU'
   '28600.0/16060::Lipscomb'
   '316.0/16060::Jacksonville'
   '2711.0/16060::North+Florida'
   '678.0/16060::Stetson'
   '1157.0/16060::Kennesaw+St.'
   '52.0/16060::Bellarmine'
   '487.0/16060::North+Ala.'
   '1004.0/16060::Central+Ark.'
   '202.0/16060::Eastern+Ky.'
   '315.0/16060::Jacksonville+St.'
   '43.0/16060::Austin+Peay'
   '11504.0/16060::Queens+%28NC%29'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
