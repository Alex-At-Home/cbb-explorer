#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=patriot
array=(
   '153.0/16500::Colgate'
   '342.0/16500::Lafayette'
   '725.0/16500::Army+West+Point'
   '23.0/16500::American'
   '369.0/16500::Loyola+Maryland'
   '726.0/16500::Navy'
   '285.0/16500::Holy+Cross'
   '83.0/16500::Bucknell'
   '352.0/16500::Lehigh'
   '68.0/16500::Boston+U.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"