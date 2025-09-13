#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=atlanticten
array=(
   '175.0/15002::Dayton'
   '575.0/15002::Richmond'
   '572.0/15002::Rhode+Island'
   '173.0/15002::Davidson'
   '609.0/15002::Saint+Louis'
   '194.0/15002::Duquesne'
   '740.0/15002::VCU'
   '596.0/15002::St.+Bonaventure'
   '606.0/15002::Saint+Joseph%27s'
   '400.0/15002::Massachusetts'
   '340.0/15002::La+Salle'
   '248.0/15002::George+Mason'
   '249.0/15002::George+Washington'
   '236.0/15002::Fordham'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
