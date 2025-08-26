#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=nec
array=(
   '81.0/15480::Bryant'
   '599.0/15480::St.+Francis+Brooklyn'
   '361.0/15480::LIU'
   '222.0/15480::Fairleigh+Dickinson'
   '127.0/15480::Central+Conn.+St.'
   '590.0/15480::Sacred+Heart'
   '748.0/15480::Wagner'
   '600.0/15480::Saint+Francis+%28PA%29'
   '450.0/15480::Mount+St.+Mary%27s'
   '410.0/15480::Merrimack'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
