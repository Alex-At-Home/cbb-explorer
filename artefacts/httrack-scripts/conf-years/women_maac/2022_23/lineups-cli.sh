#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=maac
array=(
   '639.0/16061::Siena'
   '310.0/16061::Iona'
   '617.0/16061::Saint+Peter%27s'
   '576.0/16061::Rider'
   '116.0/16061::Canisius'
   '562.0/16061::Quinnipiac'
   '386.0/16061::Marist'
   '482.0/16061::Niagara'
   '381.0/16061::Manhattan'
   '220.0/16061::Fairfield'
   '450.0/16061::Mount+St.+Mary%27s'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
