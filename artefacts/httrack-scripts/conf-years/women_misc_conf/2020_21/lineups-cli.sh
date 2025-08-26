#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=women_misc_conf
array=(
'77.0/15500::BYU'
'260.0/15500::Gonzaga'
'649.0/15500::South+Dakota+St.'
'650.0/15500::South+Dakota'
'669.0/15500::Missouri+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
