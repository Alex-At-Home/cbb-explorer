#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=maac
array=(
   '439.0/15480::Monmouth'
   '639.0/15480::Siena'
   '310.0/15480::Iona'
   '617.0/15480::Saint+Peter%27s'
   '576.0/15480::Rider'
   '116.0/15480::Canisius'
   '562.0/15480::Quinnipiac'
   '386.0/15480::Marist'
   '482.0/15480::Niagara'
   '381.0/15480::Manhattan'
   '220.0/15480::Fairfield'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
