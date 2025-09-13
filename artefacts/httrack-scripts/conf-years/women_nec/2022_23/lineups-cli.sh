#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2022
CONF=nec
array=(
   '599.0/16061::St.+Francis+Brooklyn'
   '361.0/16061::LIU'
   '222.0/16061::Fairleigh+Dickinson'
   '127.0/16061::Central+Conn.+St.'
   '590.0/16061::Sacred+Heart'
   '748.0/16061::Wagner'
   '600.0/16061::Saint+Francis+%28PA%29'
   '410.0/16061::Merrimack'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
