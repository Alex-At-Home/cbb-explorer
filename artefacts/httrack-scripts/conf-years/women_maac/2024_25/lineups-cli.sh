#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=maac
array=(
   '639.0/16720::Siena'
   '310.0/16720::Iona'
   '617.0/16720::Saint+Peter%27s'
   '576.0/16720::Rider'
   '116.0/16720::Canisius'
   '562.0/16720::Quinnipiac'
   '386.0/16720::Marist'
   '482.0/16720::Niagara'
   '381.0/16720::Manhattan'
   '220.0/16720::Fairfield'
   '450.0/16720::Mount+St.+Mary%27s'
   '590.0/16720::Sacred+Heart'
   '410.0/16720::Merrimack'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"