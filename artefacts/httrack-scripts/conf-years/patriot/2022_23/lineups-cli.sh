#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=patriot
array=(
   '153.0/16060::Colgate'
   '342.0/16060::Lafayette'
   '725.0/16060::Army+West+Point'
   '23.0/16060::American'
   '369.0/16060::Loyola+Maryland'
   '726.0/16060::Navy'
   '285.0/16060::Holy+Cross'
   '83.0/16060::Bucknell'
   '352.0/16060::Lehigh'
   '68.0/16060::Boston+U.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
