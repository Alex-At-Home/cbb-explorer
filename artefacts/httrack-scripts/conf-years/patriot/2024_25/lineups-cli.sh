#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=patriot
array=(
   '153.0/16700::Colgate'
   '342.0/16700::Lafayette'
   '725.0/16700::Army+West+Point'
   '23.0/16700::American'
   '369.0/16700::Loyola+Maryland'
   '726.0/16700::Navy'
   '285.0/16700::Holy+Cross'
   '83.0/16700::Bucknell'
   '352.0/16700::Lehigh'
   '68.0/16700::Boston+U.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"