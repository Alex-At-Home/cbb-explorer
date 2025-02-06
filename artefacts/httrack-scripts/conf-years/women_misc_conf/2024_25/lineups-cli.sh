#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

#look for <a href="/teams/history/WBB/

YEAR=2024
CONF=women_misc_conf
array=(
  '260.0/16720::Gonzaga'
  '554.0/16720::Princeton'
  '649.0/16720::South+Dakota+St.'
  '650.0/16720::South+Dakota'
  '669.0/16720::Missouri+St.'
  '28755.0/16720::FGCU'
  '528.0/16720::Oregon+St.'
  '754.0/16720::Washington+St.'
  '220.0/16720::Fairfield'
  '317.0/16720::James+Madison'
  '158.0/16720::Columbia'
  '575.0/16720::Richmond'
  '1104.0/16720::Grand+Canyon'
  '248.0/16720::George+Mason'
  '14927.0/16720::Belmont'
  '465.0/16720::UNLV'
  '419.0/16720::Middle+Tenn.'
  '47.0/16720::Ball+St.'
  '454.0/16720::Murray+St.'
  '551.0/16720::Portland'
  '811.0/16720::Wyoming'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"