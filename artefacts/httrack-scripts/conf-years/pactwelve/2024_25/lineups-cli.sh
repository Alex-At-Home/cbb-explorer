#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=pactwelve
array=(
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"

