#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=patriot
array=(
   '153.0/16501::Colgate'
   '342.0/16501::Lafayette'
   '725.0/16501::Army+West+Point'
   '23.0/16501::American'
   '369.0/16501::Loyola+Maryland'
   '726.0/16501::Navy'
   '285.0/16501::Holy+Cross'
   '83.0/16501::Bucknell'
   '352.0/16501::Lehigh'
   '68.0/16501::Boston+U.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"