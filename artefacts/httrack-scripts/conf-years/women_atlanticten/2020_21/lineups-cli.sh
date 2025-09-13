#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=atlanticten
array=(
   '609.0/15500::Saint+Louis'
   '400.0/15500::Massachusetts'
   '740.0/15500::VCU'
   '575.0/15500::Richmond'
   '572.0/15500::Rhode+Island'
   '173.0/15500::Davidson'
   '249.0/15500::George+Washington'
   '596.0/15500::St.+Bonaventure'
   '606.0/15500::Saint+Joseph%27s'
   '175.0/15500::Dayton'
   '248.0/15500::George+Mason'
   '340.0/15500::La+Salle'
   '194.0/15500::Duquesne'
   '236.0/15500::Fordham'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
