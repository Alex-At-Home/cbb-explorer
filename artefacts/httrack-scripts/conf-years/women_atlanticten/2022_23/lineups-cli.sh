#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=atlanticten
array=(
   '609.0/16061::Saint+Louis'
   '400.0/16061::Massachusetts'
   '740.0/16061::VCU'
   '575.0/16061::Richmond'
   '572.0/16061::Rhode+Island'
   '173.0/16061::Davidson'
   '249.0/16061::George+Washington'
   '596.0/16061::St.+Bonaventure'
   '606.0/16061::Saint+Joseph%27s'
   '175.0/16061::Dayton'
   '248.0/16061::George+Mason'
   '340.0/16061::La+Salle'
   '194.0/16061::Duquesne'
   '236.0/16061::Fordham'
   '371.0/16061::Loyola+Chicago'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
