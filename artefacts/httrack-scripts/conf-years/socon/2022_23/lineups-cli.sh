#!/bin/bash

#(source .lineup.env first to set up these variables)
YEAR=2022
CONF=socon
array=(
   '141.0/16060::The+Citadel'
   '244.0/16060::Furman'
   '769.0/16060::Western+Caro.'
   '741.0/16060::VMI'
   '625.0/16060::Samford'
   '406.0/16060::Mercer'
   '2915.0/16060::Wofford'
   '693.0/16060::Chattanooga'
   '459.0/16060::UNC+Greensboro'
   '198.0/16060::ETSU'
)

import_data_v1 "$YEAR" "$CONF" "${array[@]}"
