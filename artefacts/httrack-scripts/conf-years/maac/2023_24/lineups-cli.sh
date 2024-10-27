#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=maac
array=(
   '639.0/16501::Siena'
   '310.0/16501::Iona'
   '617.0/16501::Saint+Peter%27s'
   '576.0/16501::Rider'
   '116.0/16501::Canisius'
   '562.0/16501::Quinnipiac'
   '386.0/16501::Marist'
   '482.0/16501::Niagara'
   '381.0/16501::Manhattan'
   '220.0/16501::Fairfield'
   '450.0/16501::Mount+St.+Mary%27s'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"