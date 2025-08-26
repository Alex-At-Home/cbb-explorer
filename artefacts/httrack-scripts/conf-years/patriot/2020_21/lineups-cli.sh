#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=patriot
array=(
   '153.0/15480::Colgate'
   '342.0/15480::Lafayette'
   '725.0/15480::Army+West+Point'
   '23.0/15480::American'
   '369.0/15480::Loyola+Maryland'
   '726.0/15480::Navy'
   '285.0/15480::Holy+Cross'
   '83.0/15480::Bucknell'
   '352.0/15480::Lehigh'
   '68.0/15480::Boston+U.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
