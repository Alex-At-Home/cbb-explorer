#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=bigeast
array=(
   '169.0/16060::Creighton'
   '739.0/16060::Villanova'
   '603.0/16060::St.+John%27s+%28NY%29'
   '812.0/16060::Xavier'
   '635.0/16060::Seton+Hall'
   '556.0/16060::Providence'
   '176.0/16060::DePaul'
   '164.0/16060::UConn'
   '387.0/16060::Marquette'
   '251.0/16060::Georgetown'
   '87.0/16060::Butler'
)
import_data_v1 "$YEAR" "$CONF" "${array[@]}"
