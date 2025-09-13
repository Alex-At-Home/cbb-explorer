#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=atlanticten
array=(
   '609.0/16500::Saint+Louis'
   '400.0/16500::Massachusetts'
   '740.0/16500::VCU'
   '575.0/16500::Richmond'
   '572.0/16500::Rhode+Island'
   '173.0/16500::Davidson'
   '249.0/16500::George+Washington'
   '596.0/16500::St.+Bonaventure'
   '606.0/16500::Saint+Joseph%27s'
   '175.0/16500::Dayton'
   '248.0/16500::George+Mason'
   '340.0/16500::La+Salle'
   '194.0/16500::Duquesne'
   '236.0/16500::Fordham'
   '371.0/16500::Loyola+Chicago'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"