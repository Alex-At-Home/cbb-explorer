#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=maac
array=(
   '639.0/16060::Siena'
   '310.0/16060::Iona'
   '617.0/16060::Saint+Peter%27s'
   '576.0/16060::Rider'
   '116.0/16060::Canisius'
   '562.0/16060::Quinnipiac'
   '386.0/16060::Marist'
   '482.0/16060::Niagara'
   '381.0/16060::Manhattan'
   '220.0/16060::Fairfield'
   '450.0/16060::Mount+St.+Mary%27s'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
