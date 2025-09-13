#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=patriot
array=(
   '153.0/15866::Colgate'
   '342.0/15866::Lafayette'
   '725.0/15866::Army+West+Point'
   '23.0/15866::American'
   '369.0/15866::Loyola+Maryland'
   '726.0/15866::Navy'
   '285.0/15866::Holy+Cross'
   '83.0/15866::Bucknell'
   '352.0/15866::Lehigh'
   '68.0/15866::Boston+U.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
