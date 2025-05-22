#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2021
CONF=nec
array=(
   '81.0/15881::Bryant'
   '599.0/15881::St.+Francis+Brooklyn'
   '361.0/15881::LIU'
   '222.0/15881::Fairleigh+Dickinson'
   '127.0/15881::Central+Conn.+St.'
   '590.0/15881::Sacred+Heart'
   '748.0/15881::Wagner'
   '600.0/15881::Saint+Francis+%28PA%29'
   '450.0/15881::Mount+St.+Mary%27s'
   '410.0/15881::Merrimack'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
