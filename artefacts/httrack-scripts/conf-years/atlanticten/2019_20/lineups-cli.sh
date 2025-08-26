#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=atlanticten
array=(
   '175.0/15061::Dayton'
   '575.0/15061::Richmond'
   '572.0/15061::Rhode+Island'
   '173.0/15061::Davidson'
   '609.0/15061::Saint+Louis'
   '194.0/15061::Duquesne'
   '740.0/15061::VCU'
   '596.0/15061::St.+Bonaventure'
   '606.0/15061::Saint+Joseph%27s'
   '400.0/15061::Massachusetts'
   '340.0/15061::La+Salle'
   '248.0/15061::George+Mason'
   '249.0/15061::George+Washington'
   '236.0/15061::Fordham'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
