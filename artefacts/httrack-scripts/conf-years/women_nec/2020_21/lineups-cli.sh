#!/bin/bash

source $PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh

YEAR=2020
CONF=nec
array=(
   '81.0/15500::Bryant'
   '599.0/15500::St.+Francis+Brooklyn'
   '361.0/15500::LIU'
   '222.0/15500::Fairleigh+Dickinson'
   '127.0/15500::Central+Conn.+St.'
   '590.0/15500::Sacred+Heart'
   '748.0/15500::Wagner'
   '600.0/15500::Saint+Francis+%28PA%29'
   '450.0/15500::Mount+St.+Mary%27s'
   '410.0/15500::Merrimack'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
