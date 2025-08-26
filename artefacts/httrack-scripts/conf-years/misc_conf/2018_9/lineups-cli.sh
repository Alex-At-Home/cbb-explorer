#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2018
CONF=misc_conf
array=(
    '260.0/14300::Gonzaga'
    '77.0/14300::BYU'
    '610.0/14300::Saint+Mary%27s+%28CA%29'
    '731.0/14300::Utah+St.'
    '626.0/14300::San+Diego+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
