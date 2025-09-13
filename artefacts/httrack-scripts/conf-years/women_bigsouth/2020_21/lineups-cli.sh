#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=bigsouth
array=(
   '792.0/15500::Winthrop'
   '456.0/15500::UNC+Asheville'
   '115.0/15500::Campbell'
   '1092.0/15500::Gardner-Webb'
   '270.0/15500::Hampton'
   '19651.0/15500::High+Point'
   '10411.0/15500::USC+Upstate'
   '563.0/15500::Radford'
   '363.0/15500::Longwood'
   '1320.0/15500::Presbyterian'
   '48.0/15500::Charleston+So.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
