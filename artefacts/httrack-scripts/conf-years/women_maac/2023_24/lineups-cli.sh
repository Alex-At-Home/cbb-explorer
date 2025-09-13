#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=maac
array=(
   '639.0/16500::Siena'
   '310.0/16500::Iona'
   '617.0/16500::Saint+Peter%27s'
   '576.0/16500::Rider'
   '116.0/16500::Canisius'
   '562.0/16500::Quinnipiac'
   '386.0/16500::Marist'
   '482.0/16500::Niagara'
   '381.0/16500::Manhattan'
   '220.0/16500::Fairfield'
   '450.0/16500::Mount+St.+Mary%27s'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"