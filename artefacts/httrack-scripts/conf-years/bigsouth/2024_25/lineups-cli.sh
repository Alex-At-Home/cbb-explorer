#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=bigsouth
array=(
   '792.0/16700::Winthrop'
   '456.0/16700::UNC+Asheville'
   '1092.0/16700::Gardner-Webb'
   '19651.0/16700::High+Point'
   '10411.0/16700::USC+Upstate'
   '563.0/16700::Radford'
   '363.0/16700::Longwood'
   '1320.0/16700::Presbyterian'
   '48.0/16700::Charleston+So.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"