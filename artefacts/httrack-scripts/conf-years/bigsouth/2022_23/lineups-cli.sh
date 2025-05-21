#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=bigsouth
array=(
   '792.0/16060::Winthrop'
   '456.0/16060::UNC+Asheville'
   '115.0/16060::Campbell'
   '1092.0/16060::Gardner-Webb'
   '19651.0/16060::High+Point'
   '10411.0/16060::USC+Upstate'
   '563.0/16060::Radford'
   '363.0/16060::Longwood'
   '1320.0/16060::Presbyterian'
   '48.0/16060::Charleston+So.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
