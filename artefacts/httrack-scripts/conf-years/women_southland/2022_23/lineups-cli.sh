#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=southland
array=(
   '402.0/16061::McNeese'
   '483.0/16061::Nicholls'
   '2743.0/16061::UIW'
   '287.0/16061::Houston+Christian'
   '474.0/16061::New+Orleans'
   '508.0/16061::Northwestern+St.'
   '26172.0/16061::A%26M-Corpus+Christi'
   '655.0/16061::Southeastern+La.'
   '199.0/16061::Tex.+A%26M-Commerce'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
