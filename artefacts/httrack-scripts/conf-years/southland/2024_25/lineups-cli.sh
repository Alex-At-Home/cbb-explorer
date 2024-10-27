#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=southland
array=(
   '402.0/16700::McNeese'
   '483.0/16700::Nicholls'
   '2743.0/16700::UIW'
   '287.0/16700::Houston+Christian'
   '474.0/16700::New+Orleans'
   '508.0/16700::Northwestern+St.'
   '26172.0/16700::A%26M-Corpus+Christi'
   '655.0/16700::Southeastern+La.'
   '199.0/16700::Tex.+A%26M-Commerce'
   '346.0/16700::Lamar+University'
   '536.0/16700::UTRGV'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"