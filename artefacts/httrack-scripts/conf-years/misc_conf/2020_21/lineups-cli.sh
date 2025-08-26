#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=misc_conf
array=(
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
