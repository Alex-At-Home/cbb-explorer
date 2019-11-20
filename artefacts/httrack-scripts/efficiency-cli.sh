# Need to set up authentication first, ie "cookies.txt" in the working directory in the format
# (NOT the EFF_CRAWL_PATH, the _working_ path!)
# $EFF_ROOT_URL      TRUE    /       FALSE   1999999999      PHPSESSID     ADD_SESSION_ID
#EFF_CRAWL_PATH=$1
#EFF_ROOT_URL=$2
httrack "https://$EFF_ROOT_URL" --depth=3 --path $EFF_CRAWL_PATH --robots=0 -N "%h%p/%n%q%[y]_%[team]_%[c]_%[s]_%[p].%t" "-*" "+$EFF_ROOT_URL/team.php*" "+$EFF_ROOT_URL/index.php?y=*" -v
