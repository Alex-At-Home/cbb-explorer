#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=women_bigeast
array=(
   '164.0/16061::UConn'
   '176.0/16061::DePaul'
   '635.0/16061::Seton+Hall'
   '739.0/16061::Villanova'
   '387.0/16061::Marquette'
   '603.0/16061::St.+John%27s+%28NY%29'
   '812.0/16061::Xavier'
   '169.0/16061::Creighton'
   '556.0/16061::Providence'
   '87.0/16061::Butler'
   '251.0/16061::Georgetown'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
