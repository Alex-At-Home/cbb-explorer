#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=maac
array=(
   '439.0/15881::Monmouth'
   '639.0/15881::Siena'
   '310.0/15881::Iona'
   '617.0/15881::Saint+Peter%27s'
   '576.0/15881::Rider'
   '116.0/15881::Canisius'
   '562.0/15881::Quinnipiac'
   '386.0/15881::Marist'
   '482.0/15881::Niagara'
   '381.0/15881::Manhattan'
   '220.0/15881::Fairfield'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
