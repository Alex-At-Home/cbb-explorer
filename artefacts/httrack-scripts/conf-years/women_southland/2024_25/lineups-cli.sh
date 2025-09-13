#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=southland
array=(
   '402.0/16720::McNeese'
   '483.0/16720::Nicholls'
   '2743.0/16720::UIW'
   '287.0/16720::Houston+Christian'
   '474.0/16720::New+Orleans'
   '508.0/16720::Northwestern+St.'
   '26172.0/16720::A%26M-Corpus+Christi'
   '655.0/16720::Southeastern+La.'
   '199.0/16720::East+Texas+A%26M'
   '346.0/16720::Lamar+University'
   '536.0/16720::UTRGV'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"