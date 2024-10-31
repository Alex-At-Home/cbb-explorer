#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=nec
array=(
   '361.0/16700::LIU'
   '222.0/16700::FDU'
   '127.0/16700::Central+Conn.+St.'
   '748.0/16700::Wagner'
   '600.0/16700::Saint+Francis'
   '349.0/16700::Le+Moyne'
   '136.0/16700::Chicago+St.'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"