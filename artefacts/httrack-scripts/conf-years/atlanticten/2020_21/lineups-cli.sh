#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=atlanticten
array=(
   '609.0/15480::Saint+Louis'
   '400.0/15480::Massachusetts'
   '740.0/15480::VCU'
   '575.0/15480::Richmond'
   '572.0/15480::Rhode+Island'
   '173.0/15480::Davidson'
   '249.0/15480::George+Washington'
   '596.0/15480::St.+Bonaventure'
   '606.0/15480::Saint+Joseph%27s'
   '175.0/15480::Dayton'
   '248.0/15480::George+Mason'
   '340.0/15480::La+Salle'
   '194.0/15480::Duquesne'
   '236.0/15480::Fordham'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
