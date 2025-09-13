#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigsouth
array=(
   '792.0/16720::Winthrop'
   '456.0/16720::UNC+Asheville'
   '1092.0/16720::Gardner-Webb'
   '19651.0/16720::High+Point'
   '10411.0/16720::USC+Upstate'
   '563.0/16720::Radford'
   '363.0/16720::Longwood'
   '1320.0/16720::Presbyterian'
   '48.0/16720::Charleston+So.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"