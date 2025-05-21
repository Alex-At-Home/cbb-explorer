#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=women_misc_conf
array=(
'77.0/16061::BYU'
'260.0/16061::Gonzaga'
'554.0/16061::Princeton'
'649.0/16061::South+Dakota+St.'
'650.0/16061::South+Dakota'
'669.0/16061::Missouri+St.'
'28755.0/16061::FGCU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
