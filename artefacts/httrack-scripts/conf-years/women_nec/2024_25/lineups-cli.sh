#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2024
CONF=nec
array=(
   '361.0/16720::LIU'
   '222.0/16720::FDU'
   '127.0/16720::Central+Conn.+St.'
   '748.0/16720::Wagner'
   '600.0/16720::Saint+Francis'
   '349.0/16720::Le+Moyne'
   '136.0/16720::Chicago+St.'
   '408.0/16720::Mercyhurst'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"