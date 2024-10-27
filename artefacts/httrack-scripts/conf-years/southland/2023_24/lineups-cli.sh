#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=southland
array=(
   '402.0/16501::McNeese'
   '483.0/16501::Nicholls'
   '2743.0/16501::UIW'
   '287.0/16501::Houston+Christian'
   '474.0/16501::New+Orleans'
   '508.0/16501::Northwestern+St.'
   '26172.0/16501::A%26M-Corpus+Christi'
   '655.0/16501::Southeastern+La.'
   '199.0/16501::Tex.+A%26M-Commerce'
   '346.0/16501::Lamar+University'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"