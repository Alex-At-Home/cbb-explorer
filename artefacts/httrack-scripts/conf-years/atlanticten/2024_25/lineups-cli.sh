#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=atlanticten
array=(
   '609.0/16700::Saint+Louis'
   '400.0/16700::Massachusetts'
   '740.0/16700::VCU'
   '575.0/16700::Richmond'
   '572.0/16700::Rhode+Island'
   '173.0/16700::Davidson'
   '249.0/16700::George+Washington'
   '596.0/16700::St.+Bonaventure'
   '606.0/16700::Saint+Joseph%27s'
   '175.0/16700::Dayton'
   '248.0/16700::George+Mason'
   '340.0/16700::La+Salle'
   '194.0/16700::Duquesne'
   '236.0/16700::Fordham'
   '371.0/16700::Loyola+Chicago'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"