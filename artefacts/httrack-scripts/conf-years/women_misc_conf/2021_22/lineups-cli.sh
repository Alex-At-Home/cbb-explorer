#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=women_misc_conf
array=(
'77.0/15866::BYU'
'260.0/15866::Gonzaga'
'554.0/15866::Princeton'
'649.0/15866::South+Dakota+St.'
'650.0/15866::South+Dakota'
'669.0/15866::Missouri+St.'
'28755.0/15866::FGCU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
