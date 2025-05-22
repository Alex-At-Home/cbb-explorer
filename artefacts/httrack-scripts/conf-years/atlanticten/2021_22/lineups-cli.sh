#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=atlanticten
array=(
   '609.0/15881::Saint+Louis'
   '400.0/15881::Massachusetts'
   '740.0/15881::VCU'
   '575.0/15881::Richmond'
   '572.0/15881::Rhode+Island'
   '173.0/15881::Davidson'
   '249.0/15881::George+Washington'
   '596.0/15881::St.+Bonaventure'
   '606.0/15881::Saint+Joseph%27s'
   '175.0/15881::Dayton'
   '248.0/15881::George+Mason'
   '340.0/15881::La+Salle'
   '194.0/15881::Duquesne'
   '236.0/15881::Fordham'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
