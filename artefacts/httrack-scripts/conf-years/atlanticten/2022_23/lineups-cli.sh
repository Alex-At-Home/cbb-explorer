#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=atlanticten
array=(
   '609.0/16060::Saint+Louis'
   '400.0/16060::Massachusetts'
   '740.0/16060::VCU'
   '575.0/16060::Richmond'
   '572.0/16060::Rhode+Island'
   '173.0/16060::Davidson'
   '249.0/16060::George+Washington'
   '596.0/16060::St.+Bonaventure'
   '606.0/16060::Saint+Joseph%27s'
   '175.0/16060::Dayton'
   '248.0/16060::George+Mason'
   '340.0/16060::La+Salle'
   '194.0/16060::Duquesne'
   '236.0/16060::Fordham'
   '371.0/16060::Loyola+Chicago'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
