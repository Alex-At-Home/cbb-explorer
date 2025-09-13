#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=southland
array=(
   '402.0/15500::McNeese'
   '624.0/15500::Sam+Houston'
   '676.0/15500::SFA'
   '2.0/15500::Abilene+Christian'
   '483.0/15500::Nicholls+St.'
   '1004.0/15500::Central+Ark.'
   '2743.0/15500::UIW'
   '287.0/15500::Houston+Baptist'
   '474.0/15500::New+Orleans'
   '508.0/15500::Northwestern+St.'
   '346.0/15500::Lamar+University'
   '26172.0/15500::A%26M-Corpus+Christi'
   '655.0/15500::Southeastern+La.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
