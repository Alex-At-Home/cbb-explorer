#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2023
CONF=nec
array=(
   '361.0/16501::LIU'
   '222.0/16501::FDU'
   '127.0/16501::Central+Conn.+St.'
   '590.0/16501::Sacred+Heart'
   '748.0/16501::Wagner'
   '600.0/16501::Saint+Francis'
   '410.0/16501::Merrimack'
   '349.0/16501::Le+Moyne'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"