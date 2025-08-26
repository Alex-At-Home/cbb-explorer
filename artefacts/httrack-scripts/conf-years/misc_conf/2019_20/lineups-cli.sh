#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=misc_conf
array=(
  '260.0/15061::Gonzaga'
  '77.0/15061::BYU'
  '610.0/15061::Saint+Mary%27s+%28CA%29'
  '731.0/15061::Utah+St.'
  '626.0/15061::San+Diego+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
