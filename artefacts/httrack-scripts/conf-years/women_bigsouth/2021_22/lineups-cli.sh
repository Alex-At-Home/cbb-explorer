#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=bigsouth
array=(
   '792.0/15866::Winthrop'
   '456.0/15866::UNC+Asheville'
   '115.0/15866::Campbell'
   '1092.0/15866::Gardner-Webb'
   '270.0/15866::Hampton'
   '19651.0/15866::High+Point'
   '10411.0/15866::USC+Upstate'
   '563.0/15866::Radford'
   '363.0/15866::Longwood'
   '1320.0/15866::Presbyterian'
   '48.0/15866::Charleston+So.'
   '488.0/15866::N.C.+A%26T'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
