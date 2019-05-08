# Need to set up authentication first, ie "cookies.txt" in the working directory in the format
# $ROOT_URL      TRUE    /       FALSE   1999999999      PHPSESSID     ADD_SESSION_ID
CRAWL_PATH=$1
ROOT_URL=TODO
httrack "https://$ROOT_URL" --depth=3 --path $CRAWL_PATH --robots=0 -N "%h%p/%n%q%[y]_%[team]_%[c]_%[s]_%[p].%t" "-*" "+$ROOT_URL/team.php*" "+$ROOT_URL/index.php?y=*" -v
