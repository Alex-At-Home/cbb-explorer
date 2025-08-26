#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2019
CONF=women_misc_conf
array=(
'649.0/15002::South+Dakota+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
