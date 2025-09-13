#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=bigsouth
array=(
   '792.0/16061::Winthrop'
   '456.0/16061::UNC+Asheville'
   '115.0/16061::Campbell'
   '1092.0/16061::Gardner-Webb'
   '19651.0/16061::High+Point'
   '10411.0/16061::USC+Upstate'
   '563.0/16061::Radford'
   '363.0/16061::Longwood'
   '1320.0/16061::Presbyterian'
   '48.0/16061::Charleston+So.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
