#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=maac
array=(
   '439.0/15866::Monmouth'
   '639.0/15866::Siena'
   '310.0/15866::Iona'
   '617.0/15866::Saint+Peter%27s'
   '576.0/15866::Rider'
   '116.0/15866::Canisius'
   '562.0/15866::Quinnipiac'
   '386.0/15866::Marist'
   '482.0/15866::Niagara'
   '381.0/15866::Manhattan'
   '220.0/15866::Fairfield'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
