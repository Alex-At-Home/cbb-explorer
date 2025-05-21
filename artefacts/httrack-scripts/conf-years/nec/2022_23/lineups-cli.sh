#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=nec
array=(
   '599.0/16060::St.+Francis+Brooklyn'
   '361.0/16060::LIU'
   '222.0/16060::Fairleigh+Dickinson'
   '127.0/16060::Central+Conn.+St.'
   '590.0/16060::Sacred+Heart'
   '748.0/16060::Wagner'
   '600.0/16060::Saint+Francis+%28PA%29'
   '410.0/16060::Merrimack'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
