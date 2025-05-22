#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=patriot
array=(
   '153.0/15881::Colgate'
   '342.0/15881::Lafayette'
   '725.0/15881::Army+West+Point'
   '23.0/15881::American'
   '369.0/15881::Loyola+Maryland'
   '726.0/15881::Navy'
   '285.0/15881::Holy+Cross'
   '83.0/15881::Bucknell'
   '352.0/15881::Lehigh'
   '68.0/15881::Boston+U.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
