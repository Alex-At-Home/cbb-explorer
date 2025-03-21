#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=atlanticten
array=(
   '609.0/16501::Saint+Louis'
   '400.0/16501::Massachusetts'
   '740.0/16501::VCU'
   '575.0/16501::Richmond'
   '572.0/16501::Rhode+Island'
   '173.0/16501::Davidson'
   '249.0/16501::George+Washington'
   '596.0/16501::St.+Bonaventure'
   '606.0/16501::Saint+Joseph%27s'
   '175.0/16501::Dayton'
   '248.0/16501::George+Mason'
   '340.0/16501::La+Salle'
   '194.0/16501::Duquesne'
   '236.0/16501::Fordham'
   '371.0/16501::Loyola+Chicago'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"