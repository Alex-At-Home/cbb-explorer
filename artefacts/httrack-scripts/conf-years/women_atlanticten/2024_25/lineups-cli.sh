#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=atlanticten
array=(
   '609.0/16720::Saint+Louis'
   '400.0/16720::Massachusetts'
   '740.0/16720::VCU'
   '575.0/16720::Richmond'
   '572.0/16720::Rhode+Island'
   '173.0/16720::Davidson'
   '249.0/16720::George+Washington'
   '596.0/16720::St.+Bonaventure'
   '606.0/16720::Saint+Joseph%27s'
   '175.0/16720::Dayton'
   '248.0/16720::George+Mason'
   '340.0/16720::La+Salle'
   '194.0/16720::Duquesne'
   '236.0/16720::Fordham'
   '371.0/16720::Loyola+Chicago'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"