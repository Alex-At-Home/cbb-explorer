#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=patriot
array=(
   '153.0/16720::Colgate'
   '342.0/16720::Lafayette'
   '725.0/16720::Army+West+Point'
   '23.0/16720::American'
   '369.0/16720::Loyola+Maryland'
   '726.0/16720::Navy'
   '285.0/16720::Holy+Cross'
   '83.0/16720::Bucknell'
   '352.0/16720::Lehigh'
   '68.0/16720::Boston+U.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"