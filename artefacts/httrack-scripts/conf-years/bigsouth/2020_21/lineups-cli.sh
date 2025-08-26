#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=bigsouth
array=(
   '792.0/15480::Winthrop'
   '456.0/15480::UNC+Asheville'
   '115.0/15480::Campbell'
   '1092.0/15480::Gardner-Webb'
   '270.0/15480::Hampton'
   '19651.0/15480::High+Point'
   '10411.0/15480::USC+Upstate'
   '563.0/15480::Radford'
   '363.0/15480::Longwood'
   '1320.0/15480::Presbyterian'
   '48.0/15480::Charleston+So.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
