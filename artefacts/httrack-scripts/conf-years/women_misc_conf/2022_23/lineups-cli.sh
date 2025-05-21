#!/bin/bash

#(source .lineup.env first to set up these variables)
#CRAWL_PATH=TODO
#ROOT_URL=TODO
#(to get the team navigate to https://$PBP_ROOT_URL/reports/attendance?id=XXX (couln)
# pick the team, select the year, then the team id is the last bit of the URL)
YEAR=2022
CONF=women_misc_conf
array=(
'77.0/16061::BYU'
'260.0/16061::Gonzaga'
'554.0/16061::Princeton'
'649.0/16061::South+Dakota+St.'
'650.0/16061::South+Dakota'
'669.0/16061::Missouri+St.'
'28755.0/16061::FGCU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
