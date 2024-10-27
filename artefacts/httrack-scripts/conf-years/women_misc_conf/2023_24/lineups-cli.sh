#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=women_misc_conf
array=(
'260.0/16500::Gonzaga'
'554.0/16500::Princeton'
'649.0/16500::South+Dakota+St.'
'650.0/16500::South+Dakota'
'669.0/16500::Missouri+St.'
'28755.0/16500::FGCU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"