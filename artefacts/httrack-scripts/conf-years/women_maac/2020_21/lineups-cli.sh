#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=maac
array=(
   '439.0/15500::Monmouth'
   '639.0/15500::Siena'
   '310.0/15500::Iona'
   '617.0/15500::Saint+Peter%27s'
   '576.0/15500::Rider'
   '116.0/15500::Canisius'
   '562.0/15500::Quinnipiac'
   '386.0/15500::Marist'
   '482.0/15500::Niagara'
   '381.0/15500::Manhattan'
   '220.0/15500::Fairfield'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
