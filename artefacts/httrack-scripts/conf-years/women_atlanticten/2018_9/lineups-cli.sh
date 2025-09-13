#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=atlanticten
array=(
   '194.0/14320::Duquesne'
   '175.0/14320::Dayton'
   '740.0/14320::VCU'
   '173.0/14320::Davidson'
   '400.0/14320::Massachusetts'
   '606.0/14320::Saint+Joseph%27s'
   '575.0/14320::Richmond'
   '248.0/14320::George+Mason'
   '572.0/14320::Rhode+Island'
   '340.0/14320::La+Salle'
   '609.0/14320::Saint+Louis'
   '596.0/14320::St.+Bonaventure'
   '236.0/14320::Fordham'
   '249.0/14320::George+Washington'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
