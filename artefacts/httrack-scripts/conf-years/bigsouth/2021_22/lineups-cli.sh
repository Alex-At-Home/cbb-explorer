#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=bigsouth
array=(
   '792.0/15881::Winthrop'
   '456.0/15881::UNC+Asheville'
   '115.0/15881::Campbell'
   '1092.0/15881::Gardner-Webb'
   '270.0/15881::Hampton'
   '19651.0/15881::High+Point'
   '10411.0/15881::USC+Upstate'
   '563.0/15881::Radford'
   '363.0/15881::Longwood'
   '1320.0/15881::Presbyterian'
   '48.0/15881::Charleston+So.'
   '488.0/15881::N.C.+A%26T'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
