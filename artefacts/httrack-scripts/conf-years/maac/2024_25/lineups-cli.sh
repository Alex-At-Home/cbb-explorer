#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=maac
array=(
   '639.0/16700::Siena'
   '310.0/16700::Iona'
   '617.0/16700::Saint+Peter%27s'
   '576.0/16700::Rider'
   '116.0/16700::Canisius'
   '562.0/16700::Quinnipiac'
   '386.0/16700::Marist'
   '482.0/16700::Niagara'
   '381.0/16700::Manhattan'
   '220.0/16700::Fairfield'
   '450.0/16700::Mount+St.+Mary%27s'
   '590.0/16700::Sacred+Heart'
   '410.0/16700::Merrimack'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"