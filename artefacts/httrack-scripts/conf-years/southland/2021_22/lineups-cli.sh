#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=southland
array=(
   '402.0/15881::McNeese'
   '483.0/15881::Nicholls'
   '2743.0/15881::UIW'
   '287.0/15881::Houston+Baptist'
   '474.0/15881::New+Orleans'
   '508.0/15881::Northwestern+St.'
   '26172.0/15881::A%26M-Corpus+Christi'
   '655.0/15881::Southeastern+La.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
