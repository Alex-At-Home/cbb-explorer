#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=mvc
array=(
   '189.0/16060::Drake'
   '669.0/16060::Missouri+St.'
   '299.0/16060::Illinois+St.'
   '72.0/16060::Bradley'
   '659.0/16060::Southern+Ill.'
   '504.0/16060::UNI'
   '735.0/16060::Valparaiso'
   '305.0/16060::Indiana+St.'
   '219.0/16060::Evansville'
   '14927.0/16060::Belmont'
   '454.0/16060::Murray+St.'
   '302.0/16060::UIC'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
